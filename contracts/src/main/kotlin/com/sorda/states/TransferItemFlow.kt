package com.sorda.states

import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.BidContract
import com.sorda.contracts.ItemContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


/**
 * Transfer of Items from one party to another after a successful auction
 *
 */

@InitiatingFlow
@SchedulableFlow
class TransferItemFlow (
        private val newOwner: Party,
        private val itemLinearId: UniqueIdentifier,
        private val bidLinearId: UniqueIdentifier
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

        val itemCommand = Command(ItemContract.Commands.Transfer(),
                listOf(newOwner.owningKey))

        val bidCommand = Command(BidContract.Commands.CloseBid(),
                listOf(ourIdentity.owningKey))

        // Grab the item
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(itemLinearId))
        val oldItemState = serviceHub.vaultService.queryBy(ItemState::class.java, criteria).states.single()

        // Grab the last bid (so that we can consume it)
        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId))
        val oldBidState = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        // Change owner here
        val oldItem = oldItemState.state.data
        val newItem = oldItem.copy(owner = newOwner)

        // Create Transaction
        val utx = TransactionBuilder(notary = notary)
                .addInputState(oldItemState)
                .addInputState(oldBidState)
                .addOutputState(newItem, ItemContract.ID)
                .addCommand(itemCommand)
                .addCommand(bidCommand)

        val ptx = serviceHub.signInitialTransaction(utx)

        val otherPartySession = initiateFlow(newOwner)
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
//                val output = stx.tx.outputsOfType<ItemState>().single()
//                "I must receive the item" using (serviceHub.myInfo.legalIdentities.contains(output.owner))
            }
        }
        val transaction= subFlow(transactionSigner)
        val expectedId = transaction.id
        val txRecorded = subFlow(ReceiveFinalityFlow(counterpartySession, expectedId))
    }
}