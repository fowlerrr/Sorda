package com.sorda

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetListedItemsFlow
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.User
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetListedItemsFlowTests {

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
    }

    @After
    fun tearDown () {
        mockNetwork.stopNodes()
    }

    @Test
    fun `Get listed items test`() {
        val partyA = nodeA.info.legalIdentities.single()
        val partyB = nodeB.info.legalIdentities.single()

        // Parties A and B list items
        nodeA.createAndListItem("New Bike", 100.0, Instant.now()).getOrThrow()
        nodeA.createAndListItem("Nice Hat", 15.0, Instant.now()).getOrThrow()
        nodeB.createAndListItem("Car", 999.0, Instant.now()).getOrThrow()

        // Party C runs get item flow
        val listedItems = nodeC.startFlow(GetListedItemsFlow.Initiator()).getOrThrow()

        assert(listedItems.size == 3)
        listedItems.map { it.description }.containsAll(listOf("New Bike", "Nice Hat", "Car"))
    }

    private fun StartedMockNode.createAndListItem(description: String, price: Double, expiry: Instant) : CordaFuture<SignedTransaction> {
        val itemFuture = startFlow(CreateAndListItemFlow(
                description = description, lastPrice = price, expiry = expiry
        ))
        mockNetwork.runNetwork()
        return itemFuture
    }

    @Test
    fun `get items from all nodes`() {
        driver(
            DriverParameters(
                startNodesInProcess = true
            )
        ) {
            val user = User("mark", "dadada", setOf(Permissions.all()))
            val aliceNode = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            val bobNode = startNode(providedName = BOB_NAME, rpcUsers = listOf(user)).getOrThrow()

            val alice = aliceNode.nodeInfo.identityAndCertFromX500Name(ALICE_NAME)
            val bob = bobNode.nodeInfo.identityAndCertFromX500Name(BOB_NAME)

            aliceNode.rpc.startFlowDynamic(
                CreateAndListItemFlow::class.java,
                "red bike",
                10.0,
                Instant.now().plus(10, ChronoUnit.MINUTES)
            )
            aliceNode.rpc.startFlowDynamic(
                CreateAndListItemFlow::class.java,
                "blue bike",
                10.0,
                Instant.now().plus(10, ChronoUnit.MINUTES)
            )

            val items = aliceNode.rpc.startFlowDynamic(GetListedItemsFlow.Initiator::class.java).returnValue.getOrThrow()
            Assertions.assertThat(items.size).isEqualTo(2)
        }
    }
}

