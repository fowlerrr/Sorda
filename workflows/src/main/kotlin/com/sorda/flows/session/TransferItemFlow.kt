package com.sorda.flows.session

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.ItemContract
import com.sorda.states.ItemState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


/**
 * Transfer of Items from one party to another after a successful auction
 *
 */

@StartableByService
@InitiatingFlow
@StartableByRPC
class TransferItemFlow (
        private val newOwner: Party,
        private val item: ItemState
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

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val otherPartySession = initiateFlow(newOwner)

        val command = Command(ItemContract.Commands.Transfer(), listOf(newOwner.owningKey))

//        val stateAndRef = serviceHub.toStateAndRef<ItemState>(item)

        // Change owner here
//        val oldItem = stateAndRef.state.data
//        val newItem = oldItem.copy(owner = newOwner, participants = oldItem.participants + newOwner)
        val newItem = item.copy(owner = newOwner, participants = item.participants + newOwner)
        // Create Transaction
        val utx = TransactionBuilder(notary = notary)
//            .addInputState(stateAndRef)
            .addOutputState(newItem, ItemContract.ID)
            .addCommand(command)

        val ptx = serviceHub.signInitialTransaction(utx)

        val stx = subFlow(CollectSignaturesFlow(
                ptx,
                listOf(otherPartySession))
        )

        // sessions with the non-local participants
        return subFlow(FinalityFlow(stx, listOf(),
                END.childProgressTracker()))

    }
}


@InitiatedBy(TransferItemFlow::class)
class TransferItemFlowResponder(
        private val counterpartySession: FlowSession
) : FlowLogic <Unit> () {

    @Suspendable
    override fun call() {

        val transactionSigner: SignTransactionFlow
        transactionSigner = object : SignTransactionFlow(counterpartySession) {
            @Suspendable override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputsOfType<ItemState>().single()
                "I must receive the item" using (serviceHub.myInfo.legalIdentities.contains(output.owner))
            }
        }
        val transaction= subFlow(transactionSigner)
        val expectedId = transaction.id
        val txRecorded = subFlow(ReceiveFinalityFlow(counterpartySession, expectedId))
    }
}




