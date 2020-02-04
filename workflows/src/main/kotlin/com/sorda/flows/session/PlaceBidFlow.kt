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
class PlaceBidFlow(private val issuer: Party,
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

        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        val oldBidStateStateAndRef = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()
        val oldBidState = oldBidStateStateAndRef.state.data

//        // Decide what to do
        if (offer.offerPrice.SORDA > oldBidState.lastPrice) {
            /** Accept offer */
            // Update BidState
            val oldLastSuccessfulBidder = oldBidState.lastSuccessfulBidder
            val newLastSuccessfulBidder = ourIdentity
            val newBidState = oldBidState.copy(lastPrice = offer.offerPrice.SORDA, lastSuccessfulBidder = newLastSuccessfulBidder)

            // Create Transaction
            val signers = if (oldLastSuccessfulBidder == issuer) listOf(issuer.owningKey)
                                    else listOf(issuer.owningKey, oldLastSuccessfulBidder.owningKey, newLastSuccessfulBidder.owningKey)
            val bidCommand =
                    Command(BidContract.Commands.PlaceBid(),
                    signers)

            val notary = serviceHub.networkMapCache.notaryIdentities.single()
            val utx = TransactionBuilder(notary = notary)
                    .addInputState(oldBidStateStateAndRef)
                    .addOutputState(newBidState, BidContract.ID)
                    .addCommand(bidCommand)

            val ptx = serviceHub.signInitialTransaction(utx)


           if (oldLastSuccessfulBidder == issuer) {
                val stx = subFlow(CollectSignaturesFlow(
                        ptx,
                        listOf(issuerSession))
                )

                subFlow(FinalityFlow(stx, listOf(issuerSession),
                    AcceptOrRejectBidFlow.Companion.END.childProgressTracker()))
            }
            else {
               val oldBidderSession = initiateFlow(oldLastSuccessfulBidder)
               //val newBidderSession = initiateFlow(issuer)

               val stx = subFlow(CollectSignaturesFlow(
                       ptx,
                       listOf(issuerSession, oldBidderSession))
               )

               subFlow(FinalityFlow(stx, listOf(issuerSession, oldBidderSession),
                       AcceptOrRejectBidFlow.Companion.END.childProgressTracker()))
            }
//
//            // sessions with the non-local participants
//            subFlow(FinalityFlow(stx, listOfSigners.map{ initiateFlow(it) },
//                    AcceptOrRejectBidFlow.Companion.END.childProgressTracker()))
//        }
//        else {
//            /** Reject offer */
//            // Do nothing
        }
    }
}

@InitiatedBy(PlaceBidFlow::class)
class AcceptOrRejectBidFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {

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

        // Handle collect flow and Finality

        val transactionSigner: SignTransactionFlow
        transactionSigner = object : SignTransactionFlow(counterpartySession) {
            @Suspendable override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //TODO: uncomment this
//                val output = stx.tx.outputsOfType<ItemState>().single()
//                "I must receive the item" using (serviceHub.myInfo.legalIdentities.contains(output.owner))
            }
        }
        val transaction= subFlow(transactionSigner)
        val expectedId = transaction.id
        val txRecorded = subFlow(ReceiveFinalityFlow(counterpartySession, expectedId))
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