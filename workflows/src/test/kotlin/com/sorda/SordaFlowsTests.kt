package com.sorda

import com.sorda.flows.tokens.IssueSordaTokens
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.concurrent.CordaFuture
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SordaFlowsTests {

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

        nodeB = mockNetwork.createNode(
                MockNodeParameters(legalName = CordaX500Name.parse("O=NodeA,L=London,C=GB")))
        nodeA = mockNetwork.createNode(
                MockNodeParameters(legalName = CordaX500Name.parse("O=NodeB,L=London,C=GB")))

        mockNetwork.runNetwork()
    }

    fun issueTokens (party: Party, quantity: Double) : CordaFuture<SignedTransaction> {
        val d = nodeA.startFlow(IssueSordaTokens(quantity = quantity))
        mockNetwork.runNetwork()
        return d
    }

    @Test
    fun `Sorda Test`() {
        val nodeA = nodeA.info.legalIdentities.single()
        val nodeB = nodeB.info.legalIdentities.single()

        // issue tokens
        val txIssue1 = issueTokens(nodeA, 600.0).getOrThrow()
        val fungibleToken1 = txIssue1.coreTransaction.outputsOfType<FungibleToken>().single()
        assertEquals (fungibleToken1.amount.quantity, 600L)

        mockNetwork.stopNodes()
    }

}

