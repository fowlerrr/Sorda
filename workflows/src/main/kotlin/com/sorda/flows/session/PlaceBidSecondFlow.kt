package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.BidContract
import com.sorda.states.BidState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import utils.SORDA

/**
 * Place bid with the issuer of the item.
 * This can either be rejected if the price is not above the lastPrice, or accepted, in which case the BidState gets
 * updated and the previous Bid is lost
 */
@StartableByService
@StartableByRPC
@InitiatingFlow
class PlaceBidSecondFlow(private val issuer: Party,
                   private val bidLinearId: UniqueIdentifier,
                   private val offerPrice: Double
) : FlowLogic<Unit>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call()  {

        val issuerSession = initiateFlow(issuer)

        /** At this point, Issuer has already received the offer */

        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        val oldBidStateStateAndRef = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()
        val oldBidState = oldBidStateStateAndRef.state.data

        // Only update Bid when the new price is higher
        if (offerPrice.SORDA > oldBidState.lastPrice) {
            /** Accept offer */
            // Update BidState
            val oldLastSuccessfulBidder = oldBidState.lastSuccessfulBidder
            val newLastSuccessfulBidder = ourIdentity
            val newBidState = oldBidState.copy(lastPrice = offerPrice.SORDA, lastSuccessfulBidder = newLastSuccessfulBidder)

            // Create Transaction
            val signers = if (oldLastSuccessfulBidder == issuer) listOf(issuer.owningKey, newLastSuccessfulBidder.owningKey)
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

            // If the no previous Bid has been accepted, the Issuer is still the lastSuccessfulBidder on the bid
            // Then only the Issuer + Us have to sign
            if (oldLastSuccessfulBidder == issuer) {
                val stx = subFlow(CollectSignaturesFlow(
                        ptx,
                        listOf(issuerSession))
                )

                subFlow(FinalityFlow(stx, listOf(issuerSession),
                        END.childProgressTracker()))
            }
            else {
                val oldBidderSession = initiateFlow(oldLastSuccessfulBidder)

                val stx = subFlow(CollectSignaturesFlow(
                       ptx,
                       listOf(issuerSession, oldBidderSession))
                )

                subFlow(FinalityFlow(stx, listOf(issuerSession, oldBidderSession),
                        END.childProgressTracker()))
            }
        }
    }
}

@InitiatedBy(PlaceBidSecondFlow::class)
class AcceptOrRejectBidSecondFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {

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
}