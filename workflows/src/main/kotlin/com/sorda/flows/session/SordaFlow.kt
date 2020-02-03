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
class SordaFlow (
        private val party: Party
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

        val state = SordaState(party = party, id = UniqueIdentifier(), amount = 0.SORDA)


        // with input state with command
        val utx = TransactionBuilder(notary = notary)
                .addOutputState(state, SordaContract.ID)
                .addCommand(command)

        val ptx = serviceHub.signInitialTransaction(utx,
                listOf(party.owningKey)
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


@InitiatedBy(SordaFlow::class)
class StartParkingHandshakeFlowResponder(
        private val counterpartySession: FlowSession
) : FlowLogic <Unit> () {

    @Suspendable
    override fun call() {

        val transactionSigner: SignTransactionFlow
        transactionSigner = object : SignTransactionFlow(counterpartySession) {
            @Suspendable override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                Example:
//                val tx = stx.tx
//                val commands = tx.commands
//                "There must be exactly one command" using (commands.size == 1)
            }
        }
        val transaction= subFlow(transactionSigner)
        val expectedId = transaction.id
        val txRecorded = subFlow(ReceiveFinalityFlow(counterpartySession, expectedId))
    }
}




