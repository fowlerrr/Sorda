package com.sorda

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetAllItemsFlow
import com.sorda.flows.session.GetListedItemsFlow
import com.sorda.flows.tokens.IssueSordaTokens
import com.sorda.states.ItemState
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Thread.sleep
import java.time.Instant
import kotlin.test.assertEquals

class TransferItemFlowTests {

    private var notaryName = "O=Notary,L=London,C=GB"
    private var minimumPlatformVersion = 4

    lateinit var mockNetwork : MockNetwork
    lateinit var nodeA : StartedMockNode
    lateinit var nodeB : StartedMockNode

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

        mockNetwork.runNetwork()
    }

    @After
    fun tearDown () {
        mockNetwork.stopNodes()
    }

    @Test
    fun `Create And List Item that immediately expires Test`() {
        val partyA = nodeA.info.legalIdentities.single()
        val partyB = nodeB.info.legalIdentities.single()

        // Create new item and listing for new item
        val item = createAndListItem(
            nodeA, mockNetwork,
            "Our Item",
            10.0,
            Instant.now()
        ).getOrThrow()
        sleep(1_000)
        val newItemState = item.coreTransaction.outputsOfType<ItemState>().single()
        assertEquals(newItemState.owner, partyA)


//        val transfer = nodeA.startFlow(TransferItemFlow(partyB, newItemState.linearId, ))

    }


    @Test
    fun `unbid item returns to issuer after expiry`() {
        driver(
            DriverParameters(
                startNodesInProcess = true,
                extraCordappPackagesToScan = listOf("com.r3.corda.lib.tokens.contracts")
                )
        ) {
            val user = User("mark", "dadada", setOf(Permissions.all()))
            val aliceNode = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()

            aliceNode.rpc.startFlowDynamic(IssueSordaTokens::class.java, 1000.0)

            val txn = aliceNode.rpc.startFlowDynamic(
                CreateAndListItemFlow::class.java,
                "red bike",
                10.0,
                Instant.now()
            ).returnValue.getOrThrow()

            val item = txn.coreTransaction.outputsOfType(ItemState::class.java).single()
            assertThat(item.owner).isEqualTo(aliceNode.nodeInfo.legalIdentities.first())


            val items = aliceNode.rpc.startFlowDynamic(GetListedItemsFlow.Initiator::class.java).returnValue.getOrThrow()
            assertThat(items.toSet()).isEmpty()

            val allItems = aliceNode.rpc.startFlowDynamic(GetAllItemsFlow.Initiator::class.java).returnValue.getOrThrow()
            assertThat(allItems.toSet()).containsAll(listOf(item))
        }
    }
}

