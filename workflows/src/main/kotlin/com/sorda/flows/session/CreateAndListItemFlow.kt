package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import com.sorda.contracts.SordaContract
import com.sorda.states.BidState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant


/**
 * Some Sorda Flow
 *
 */

@StartableByService
@InitiatingFlow
@StartableByRPC
class CreateAndListItemFlow (
        val description: String,
        val lastPrice: Amount<TokenType>,
        val expiry: Instant
) : FlowLogic<SignedTransaction>()  {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        // Create Transaction

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // list of signers
        val command = Command(SordaContract.Commands.Start(), listOf())

        // TODO: Replace with ItemState()
        val itemState = Unit
        // TODO: Plug in ItemState()
        val bidState = subFlow(ListItemFlow(description = description,
                                                    lastPrice = lastPrice,
                                                    expiry = expiry))


        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                .addOutputState(bidState, BidContract.ID)
                .addCommand(command)
        // TODO: Add ListItem command

        val ptx = serviceHub.signInitialTransaction(utx,
                listOf(ourIdentity.owningKey)
        )

        val stx = subFlow(CollectSignaturesFlow(
                ptx,
                listOf())
        )

        // sessions with the non-local participants
        return subFlow(FinalityFlow(stx, listOf(),
                END.childProgressTracker()))

    }
}

class ListItemFlow(private val description: String,
                   private val lastPrice: Amount<TokenType>,
                   private val expiry: Instant) : FlowLogic<BidState>() {
    @Suspendable
    override fun call() : BidState{
        // Create Transaction

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // list of signers
        val command = Command(BidContract.Commands.List(), listOf())

        val bidState = BidState(description = description,
                                issuer = ourIdentity,
                                lastSuccessfulBidder = ourIdentity,
                                lastPrice = lastPrice,
                                expiry = expiry)

        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                .addOutputState(bidState, BidContract.ID)
                .addCommand(command)

        return bidState
    }
}

class CreateItemFlow() : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Create Transaction

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // list of signers
        val command = Command(BidContract.Commands.List(), listOf())

        val itemState = Unit

        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                .addOutputState(itemState, BidContract.ID)
                .addCommand(command)

        return itemState
    }
}