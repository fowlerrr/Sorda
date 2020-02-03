package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant


/**
 * Self-issue BidState and self-sign
 */
class ListItemFlow(private val item: ItemState,
                   private val lastPrice: Amount<TokenType>,
                   private val expiry: Instant,
                   private val itemLinearId: UniqueIdentifier) : FlowLogic<BidState>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call() : BidState {
        // Create Transaction

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // list of signers
        val bidCommand = Command(BidContract.Commands.List(), listOf())

        // Item can be listed
        val bidState = BidState(description = item.name,
                issuer = item.owner,
                lastSuccessfulBidder = ourIdentity,
                lastPrice = lastPrice,
                expiry = expiry,
                itemLinearId = item.linearId)

        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                // .addInputState would add the input states but there's no input state for an issuance
                .addOutputState(bidState, BidContract.ID)
                .addCommand(bidCommand)

        val ptx = serviceHub.signInitialTransaction(utx,
                listOf(ourIdentity.owningKey)
        )

        val stx = subFlow(CollectSignaturesFlow(
                ptx,
                listOf())
        )

        // sessions with the non-local participants
        subFlow(FinalityFlow(stx, listOf(),
                END.childProgressTracker()))

        return bidState
    }
}






