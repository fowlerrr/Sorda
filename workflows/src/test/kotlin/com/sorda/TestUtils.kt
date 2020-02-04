package com.sorda

import com.sorda.flows.session.CreateAndListItemFlow
import net.corda.core.concurrent.CordaFuture
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import java.time.Instant

fun createAndListItem (issuerNode: StartedMockNode,
                       mockNetwork: MockNetwork,
                       description: String,
                       price: Double,
                       expiry: Instant) : CordaFuture<SignedTransaction> {
    val d = issuerNode.startFlow(CreateAndListItemFlow(
            description = description, lastPrice = price, expiry = expiry
    ))
    mockNetwork.runNetwork()
    return d
}