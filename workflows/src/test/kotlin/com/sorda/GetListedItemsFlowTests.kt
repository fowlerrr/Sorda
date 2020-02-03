package com.sorda

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetListedItemsFlow
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.CordaX500Name
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
import java.time.Instant

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

}

