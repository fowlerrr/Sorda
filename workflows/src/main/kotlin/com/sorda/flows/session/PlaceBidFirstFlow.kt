package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.BidContract
import com.sorda.states.BidState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import utils.SORDA
import java.time.Instant

/**
 * Place bid with the issuer of the item.
 * This can either be rejected if the price is not above the lastPrice, or accepted, in which case the BidState gets
 * updated and the previous Bid is lost
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class PlaceBidFirstFlow
    (private val issuer: Party,
     private val bidLinearId: UniqueIdentifier,
     private val offerPrice: Double
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call()  {
        // Notify issuer of the offer
        val issuerSession = initiateFlow(issuer)
        val offer = Offer(bidLinearId, offerPrice)
        issuerSession.send(offer)

        val tx = subFlow(ReceiveTransactionFlow(otherSideSession = issuerSession, statesToRecord = StatesToRecord.ALL_VISIBLE ))

        assert(tx.coreTransaction.outputsOfType<BidState>().single().linearId == bidLinearId)

    }
}

@InitiatedBy(PlaceBidFirstFlow::class)
class AcceptOrRejectBidFirstFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call() {

        // Get the offer
        val offer = counterpartySession.receive<Offer>().unwrap { it }
        val oldBid = getPayload(offer.bidLinearId)
        val oldBidTxId = oldBid.ref.txhash

        val tx = serviceHub.validatedTransactions.getTransaction(oldBidTxId)
                ?: throw FlowException ("Transction not found $oldBidTxId")
        subFlow(SendTransactionFlow(counterpartySession, tx))
    }

    private fun getPayload(bidLinearId: UniqueIdentifier): StateAndRef<BidState> {
        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        val bidState = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        if (bidState.state.data.expiry < Instant.now()) {
            // TODO: is this the best Exception?
            throw FlowException("Item is expired")
        }

        return bidState
    }
}

@CordaSerializable
data class Offer(val bidLinearId: UniqueIdentifier,
                 val offerPrice: Double)