package com.sorda.states

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.sorda.contracts.BidContract
import com.sorda.contracts.ItemContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SchedulableFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


/**
 * End the Auction, initiated on the Issuer Node.
 *
 * Transfer of Items from one party to another and closing the Bid after a successful auction
 */

@InitiatingFlow
@SchedulableFlow
class TransferItemFlow (
        ///private val newOwner: Party,
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

        val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        val bidState = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        val newOwner = bidState.state.data.lastSuccessfulBidder

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val itemCommand = Command(ItemContract.Commands.Transfer(),
                listOf(newOwner.owningKey))

        // TODO: check signers if nobody has bid on it
        val bidCommand = Command(BidContract.Commands.CloseBid(),
                listOf(ourIdentity.owningKey, newOwner.owningKey))

        // Grab the item
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(itemLinearId))
        val oldItemState = serviceHub.vaultService.queryBy(ItemState::class.java, criteria).states.single()

        // Grab the last bid (so that we can consume it)
        //val bidCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(bidLinearId), status = Vault.StateStatus.UNCONSUMED)
        //val oldBidState = serviceHub.vaultService.queryBy(BidState::class.java, bidCriteria).states.single()

        // Change owner here
        val oldItem = oldItemState.state.data
        val newItem = oldItem.copy(owner = newOwner)

        // Create Transaction
        val utx = TransactionBuilder(notary = notary)
                .addInputState(oldItemState)
                .addInputState(bidState)
                .addOutputState(newItem, ItemContract.ID)
                .addCommand(itemCommand)
                .addCommand(bidCommand)

        addMoveFungibleTokens(
                transactionBuilder = utx,
                serviceHub = serviceHub,
                partiesAndAmounts = listOf(PartyAndAmount(oldItem.owner, bidState.state.data.lastPrice)),
                changeHolder = newOwner)

        val ptx = serviceHub.signInitialTransaction(utx)

//        if (newOwner == ourIdentity) {
//            return subFlow(FinalityFlow(ptx, listOf(), END.childProgressTracker()))
//        }

        val otherPartySession = initiateFlow(newOwner)
        val stx = subFlow(CollectSignaturesFlow(
                ptx,
                listOf(otherPartySession))
        )

        // sessions with the non-local participants
        return subFlow(FinalityFlow(stx, listOf(otherPartySession), END.childProgressTracker()))
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