package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.states.ItemState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.unwrap


/**
 * Get all available items on the node
 */
@StartableByRPC
@InitiatingFlow
class GetItemsFlow() : FlowLogic<List<ItemState>>() {
    @Suspendable
    override fun call() : List<ItemState> {
        // Grab the items
        var items = serviceHub.vaultService.queryBy(ItemState::class.java).states.map { it.state.data }
        serviceHub.networkMapCache.allNodes.filter { !it.isLegalIdentity(ourIdentity) }.forEach {
            val otherParty = initiateFlow(it.legalIdentities.first())
            items = items + otherParty.sendAndReceive<List<ItemState>>(Unit).unwrap { item -> item }
        }
        return items
    }
}

@InitiatedBy(GetItemsFlow::class)
class InterNodeItemsFlow(
    private val counterpartySession: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        counterpartySession.receive<Unit>()
        counterpartySession.send(serviceHub.vaultService.queryBy(ItemState::class.java).states.map { it.state.data })
    }
}






