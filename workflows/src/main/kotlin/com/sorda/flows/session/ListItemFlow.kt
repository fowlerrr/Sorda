package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.SordaContract
import com.sorda.states.SordaState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import utils.SORDA


/**
 * Some Sorda Flow
 *
 */

@StartableByService
@InitiatingFlow
@StartableByRPC
class ListItemFlow (
        private val description: String
) : FlowLogic<Unit>()  {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object START : ProgressTracker.Step("Starting")
        object END : ProgressTracker.Step("Ending") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(START, END)
    }

    @Suspendable
    override fun call(): Unit {



    }
}






