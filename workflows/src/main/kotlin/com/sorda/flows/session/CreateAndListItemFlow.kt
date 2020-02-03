package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import com.sorda.contracts.ItemContract
import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
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
        private val lastPrice: Amount<TokenType>,
        private val expiry: Instant
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
    override fun call() {
        // Create Item
        val itemState = subFlow(CreateItemFlow(description = description))

        // Create Item listing to Bid on
        val bidState = subFlow(ListItemFlow(item = itemState,
                                                    lastPrice = lastPrice,
                                                    expiry = expiry))
    }
}


