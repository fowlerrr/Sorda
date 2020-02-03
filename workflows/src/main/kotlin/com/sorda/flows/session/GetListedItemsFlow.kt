package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.states.BidState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.unwrap
import java.time.Instant

object GetListedItemsFlow {

    /**
     * Initiating flow to retrieve all currently listed items from every participant on the network.
     *
     * Currently does this naively by initiating separate flow session with every participant. This could
     * be eventually improved via only asking for updates, a gossip protocol or pushing updates.
     */
    @InitiatingFlow
    @StartableByRPC
    class Initiator : FlowLogic<List<BidState>>() {

        @Suspendable
        override fun call(): List<BidState> {
            val notaryIdentities = serviceHub.networkMapCache.notaryIdentities.toSet()

            return getPayload(serviceHub) + serviceHub.networkMapCache.allNodes.filter {
                // Remove ourselves and notary identities
                it != serviceHub.myInfo && it.legalIdentities.none { identity -> notaryIdentities.contains(identity) }
            }.map {
                val party = it.legalIdentities.last()
                val flowSession = initiateFlow(party)
                flowSession.receive<List<BidState>>().unwrap { it }
            }.flatten()
        }
    }

    /**
     * Responds to a request from
     */
    @InitiatedBy(Initiator::class)
    class Acceptor(val requesterSession: FlowSession): FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val payload = getPayload(serviceHub)
            requesterSession.send(payload)
        }
    }

    fun getPayload(serviceHub: ServiceHub): List<BidState> {
        return serviceHub.vaultService.queryBy(BidState::class.java).states.map {
            it.state.data
        }.filter {
            it.expiry > Instant.now()
        }

    }
}






