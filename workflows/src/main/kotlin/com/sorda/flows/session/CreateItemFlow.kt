package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.ItemContract
import com.sorda.states.ItemState
import net.corda.core.contracts.Command
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


/**
 * Self-issue ItemState and self-sign
 */
class CreateItemFlow(private val description: String) : FlowLogic<ItemState>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call() : ItemState {
        // Create Transaction

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // list of signers
        val command = Command(ItemContract.Commands.Issue(), listOf())

        val itemState = ItemState(owner = ourIdentity, name = description)

        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                // .addInputState would add the input states but there's no input state for an issuance
                .addOutputState(itemState, ItemContract.ID)
                .addCommand(command)

        // Verify Tx
        utx.verify(serviceHub)

        // Sign tx (Issuer signs tx)
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

        return itemState
    }
}