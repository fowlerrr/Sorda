package com.sorda

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.sorda.contracts.BidContract
import com.sorda.flows.session.*
import com.sorda.flows.tokens.IssueSordaTokens
import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.vault.QueryCriteria
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class PlaceBidFlowTest {
    private var notaryName = "O=Notary,L=London,C=GB"
    private var minimumPlatformVersion = 4

    lateinit var mockNetwork : MockNetwork
    lateinit var nodeA : StartedMockNode
    lateinit var nodeB : StartedMockNode
    lateinit var nodeC : StartedMockNode

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

//        nodeA.registerInitiatedFlow(AcceptOrRejectBidFlow::class.java)

        mockNetwork.runNetwork()
    }

    @After
    fun tearDown () {
        mockNetwork.stopNodes()
    }

    @Test
    fun `place new bid successfully`() {
        val partyA = nodeA.info.legalIdentities.single()
        val partyB = nodeB.info.legalIdentities.single()
        val partyC = nodeC.info.legalIdentities.single()

        nodeA.startFlow(IssueSordaTokens(1000.0))
        nodeB.startFlow(IssueSordaTokens(1000.0))
        nodeC.startFlow(IssueSordaTokens(1000.0))

        // Create new item and listing for new item by A
        val item = createAndListItem(nodeA, mockNetwork,"Our Item", 10.0, Instant.now().plus(1, ChronoUnit.MINUTES)).getOrThrow()
        val itemState = item.coreTransaction.outputsOfType<ItemState>().single()
        val bidLinearId = item.coreTransaction.outputsOfType<BidState>().single().linearId

        val listOfBidsFuture = nodeB.startFlow(GetListedItemsFlow.Initiator())
        mockNetwork.runNetwork()
        listOfBidsFuture.getOrThrow()

        // B places successful bid for item issued/listed by A
        val itemId = itemState.linearId
        val offerPrice = 15.0
        val future1 = nodeB.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice))
        mockNetwork.runNetwork()
        future1.getOrThrow()

        val future2 = nodeB.startFlow(PlaceBidSecondFlow(partyA, bidLinearId, offerPrice))
        mockNetwork.runNetwork()
        future2.getOrThrow()


        val offerPrice3 = 20.0
        val future3 = nodeC.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice3))
        mockNetwork.runNetwork()
        future3.getOrThrow()

        val future4 = nodeC.startFlow(PlaceBidSecondFlow(partyA, bidLinearId, offerPrice3))
        mockNetwork.runNetwork()
        future4.getOrThrow()

//        val offerPrice2 = 20.0
//        val future2 = nodeC.startFlow(PlaceBidFirstFlow(partyA, bidLinearId, offerPrice2))
//        mockNetwork.runNetwork()
//        future2.getOrThrow()




//        // B tries to find item in vault
//        val bidState = getPayload(nodeB, itemId)
//
//        assertEquals(offerPrice.SORDA, bidState.state.data.lastPrice,
//                "Last price should be updated.")
//        assertEquals(partyB, bidState.state.data.lastSuccessfulBidder,
//                "Last successful bidder should be updated.")
    }

    private fun getPayload(node: StartedMockNode, bidLinearId: UniqueIdentifier): StateAndRef<BidState> {
        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId))
        val bidState = node.services.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        if (bidState.state.data.expiry < Instant.now()) {
            // TODO: is this the best Exception?
            throw FlowException("Item is expired")
        }

        return bidState
    }

    @Test
    fun `place bid but offer is not high enough`() {

    }
}