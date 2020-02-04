package com.sorda

import com.sorda.flows.session.GetListedItemsFlow
import com.sorda.flows.session.PlaceBidFirstFlow
import com.sorda.flows.session.PlaceBidSecondFlow
import com.sorda.flows.tokens.IssueSordaTokens
import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic.Companion.sleep
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import utils.SORDA
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlaceBidFlowTest {
    private var notaryName = "O=Notary,L=London,C=GB"
    private var minimumPlatformVersion = 4

    private lateinit var mockNetwork : MockNetwork
    private lateinit var nodeA : StartedMockNode
    private lateinit var nodeB : StartedMockNode
    private lateinit var nodeC : StartedMockNode
    private lateinit var partyA : Party
    private lateinit var partyB : Party
    private lateinit var partyC : Party

    private lateinit var item : SignedTransaction
    private lateinit var itemState : ItemState
    private lateinit var bidLinearId : UniqueIdentifier

    @Before
    fun setup () {
        mockNetwork = MockNetwork(
                // legacy API is used on purpose as otherwise flows defined in tests are not picked up by the framework
                cordappPackages = listOf("com.r3.corda.lib.tokens.workflows",
                        "com.r3.corda.lib.tokens.contracts",
                        "com.r3.corda.lib.tokens.money",
                        "com.r3.corda.lib.accounts.contracts",
                        "com.r3.corda.lib.accounts.workflows",
                        "com.r3.corda.lib.ci.workflows",
                        "com.sorda.flows",
                        "com.sorda.flows.session",
                        "com.sorda.contracts",
                        "com.sorda.schema",
                        "com.sorda.schema.SordaContractsSchemaV1"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name.parse(notaryName))),
                networkParameters = testNetworkParameters(minimumPlatformVersion = minimumPlatformVersion)
        )

        nodeA = mockNetwork.createNode(
                MockNodeParameters(legalName = CordaX500Name.parse("O=NodeA,L=London,C=GB")))
        nodeB = mockNetwork.createNode(
                MockNodeParameters(legalName = CordaX500Name.parse("O=NodeB,L=London,C=GB")))
        nodeC = mockNetwork.createNode(
                MockNodeParameters(legalName = CordaX500Name.parse("O=NodeC,L=London,C=GB")))

        mockNetwork.runNetwork()

        // Everybody gets enough tokens
        nodeA.startFlow(IssueSordaTokens(1000.0))
        nodeB.startFlow(IssueSordaTokens(1000.0))
        nodeC.startFlow(IssueSordaTokens(1000.0))

        // Create new item and listing for new item by A for others to bid on
        item = createAndListItem(nodeA, mockNetwork,"Our Item", 10.0, Instant.now().plus(1, ChronoUnit.MINUTES)).getOrThrow()
        itemState = item.coreTransaction.outputsOfType<ItemState>().single()
        bidLinearId = item.coreTransaction.outputsOfType<BidState>().single().linearId

        partyA = nodeA.info.legalIdentities.single()
        partyB = nodeB.info.legalIdentities.single()
        partyC = nodeC.info.legalIdentities.single()
    }

    @After
    fun tearDown () {
        mockNetwork.stopNodes()
    }

    @Test
    fun `place new bid successfully`() {
        // Check that B can pull the BidStates from A
        val listOfBidsFuture = nodeB.startFlow(GetListedItemsFlow.Initiator())
        mockNetwork.runNetwork()
        listOfBidsFuture.getOrThrow()

        // B places successful bid for item issued/listed by A
        val offerPrice1 = 15.0
        val future1 = nodeB.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice1))
        mockNetwork.runNetwork()
        future1.getOrThrow()

        val future2 = nodeB.startFlow(PlaceBidSecondFlow(partyA, bidLinearId, offerPrice1))
        mockNetwork.runNetwork()
        future2.getOrThrow()

        // B checks their vault
        val bidState1 = getPayload(nodeB, bidLinearId)
        assertEquals(offerPrice1.SORDA, bidState1.state.data.lastPrice,
                "Last price should be updated.")
        assertEquals(partyB, bidState1.state.data.lastSuccessfulBidder,
                "Last successful bidder should be updated.")

        // C places successful bid on the same item
        val offerPrice3 = 20.0
        val future3 = nodeC.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice3))
        mockNetwork.runNetwork()
        future3.getOrThrow()

        val future4 = nodeC.startFlow(PlaceBidSecondFlow(partyA, bidLinearId, offerPrice3))
        mockNetwork.runNetwork()
        future4.getOrThrow()

        // C checks their vault
        val bidState3 = getPayload(nodeC, bidLinearId)
        assertEquals(offerPrice3.SORDA, bidState3.state.data.lastPrice,
                "Last price should be updated to $.")
        assertEquals(partyC, bidState3.state.data.lastSuccessfulBidder,
                "Last successful bidder should be updated.")

        sleep(Duration.ofMinutes(1))

        // Check

        val finalItem = getItem(nodeC, itemState.linearId).state.data

        assertEquals(partyC, finalItem.owner)
    }

    private fun getPayload(node: StartedMockNode, bidLinearId: UniqueIdentifier): StateAndRef<BidState> {
        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        val bidState = node.services.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        if (bidState.state.data.expiry < Instant.now()) {
            // TODO: is this the best Exception?
            throw FlowException("Item is expired")
        }

        return bidState
    }

    private fun getItem(node: StartedMockNode, itemLinearId: UniqueIdentifier): StateAndRef<ItemState> {
        val itemCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(itemLinearId), status = Vault.StateStatus.UNCONSUMED)
        val itemState = node.services.vaultService.queryBy(ItemState::class.java, itemCriteria).states.single()

        return itemState
    }

    @Test
    fun `place bid but offer is not high enough`() {
        // B places unsuccessful bid for item issued/listed by A
        val offerPrice1 = 9.0
        val future1 = nodeB.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice1))
        mockNetwork.runNetwork()
        future1.getOrThrow()

        val future2 = nodeB.startFlow(PlaceBidSecondFlow(partyA, bidLinearId, offerPrice1))
        mockNetwork.runNetwork()
        future2.getOrThrow()

        // B checks their vault
        val bidState1 = getPayload(nodeB, bidLinearId)
        assertNotEquals(offerPrice1.SORDA, bidState1.state.data.lastPrice,
                "Last price should not be updated.")
        assertEquals(partyA, bidState1.state.data.lastSuccessfulBidder,
                "Last successful bidder should not be updated.")
    }
}