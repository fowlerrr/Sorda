package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.BidContract
import com.sorda.contracts.ExpiryContract
import com.sorda.contracts.ItemContract
import com.sorda.states.BidState
import com.sorda.states.ExpiryState
import com.sorda.states.ItemState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import utils.SORDA
import java.time.Instant


/**
 * Create and List an Item to bid on.
 * Self-issue the item as ItemState, then create the BidState from that ItemState
 */

@StartableByService
@InitiatingFlow
@StartableByRPC
class CreateAndListItemFlow (
        private val description: String,
        private val lastPrice: Double,
        private val expiry: Instant = Instant.now()
) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call() : SignedTransaction {
        // Create Item
//        val itemState = subFlow(CreateItemFlow(description = description))
//
//        // Create Item listing to Bid on
//        val bidState = subFlow(ListItemFlow(item = itemState,
//                                    lastPrice = lastPrice.SORDA,
//                                    expiry = expiry,
//                                    itemLinearId = itemState.linearId))




        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // Bid
        val bidCommand = Command(BidContract.Commands.List(), listOf(ourIdentity.owningKey))
        val expiryCommand = Command(ExpiryContract.Commands.List(), listOf(ourIdentity.owningKey))
        val itemCommand = Command(ItemContract.Commands.Issue(), listOf(ourIdentity.owningKey))

        val itemState = ItemState(owner = ourIdentity, name = description)

        val bidState = BidState(
                description = description,
                issuer = ourIdentity,
                lastSuccessfulBidder = ourIdentity,
                lastPrice = lastPrice.SORDA,
                expiry = expiry,
                itemLinearId = itemState.linearId)

        val expiryState = ExpiryState(
                description = description,
                issuer = ourIdentity,
                expiry = expiry,
                bidLinearId = bidState.linearId,
                itemLinearId = itemState.linearId)

        println("Writing $bidState")

        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                // .addInputState would add the input states but there's no input state for an issuance
                .addOutputState(itemState, ItemContract.ID)
                .addCommand(itemCommand)
                .addOutputState(bidState, BidContract.ID)
                .addOutputState(expiryState, ExpiryContract.ID)
                .addCommand(bidCommand)
                .addCommand(expiryCommand)



        // Verify Tx
        utx.verify(serviceHub)

        // Sign tx (Issuer signs tx)
        val ptx = serviceHub.signInitialTransaction(utx,
                listOf(ourIdentity.owningKey)
        )

//        val stx = subFlow(CollectSignaturesFlow(
//                ptx,
//                listOf())
//        )

        // sessions with the non-local participants
        subFlow(FinalityFlow(ptx, listOf(),
                END.childProgressTracker()))

        val utx2 = TransactionBuilder(notary = notary)
                // .addInputState would add the input states but there's no input state for an issuance
                .addOutputState(expiryState, ExpiryContract.ID)
                .addCommand(expiryCommand)

        val ptx2 = serviceHub.signInitialTransaction(utx2,
                listOf(ourIdentity.owningKey)
        )

        return subFlow(FinalityFlow(ptx2, listOf(),
                END.childProgressTracker()))

    }
}


