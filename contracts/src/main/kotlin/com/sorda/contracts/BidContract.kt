package com.sorda.contracts

import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import utils.SORDA

class BidContract: Contract {
    companion object {
        @JvmStatic
        val ID = "com.sorda.contracts.BidContract"
    }

    interface Commands : CommandData {
        class List : TypeOnlyCommandData(), Commands
        class PlaceBid : TypeOnlyCommandData(), Commands
        class CloseBid : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        fun verifySingleInputState() {
            requireThat {
                val bidState= tx.inputsOfType<BidState>()
                "This contract supports only a single input state" using (bidState.size == 1)
                "There should be one input state of type BidState" using (bidState.single() is BidState)
            }
        }

        fun verifySingleOutputState() {
            requireThat {
                val bidState= tx.outputsOfType<BidState>()
                "This contract supports only a single output state" using (bidState.size == 1)
                "There should be one output state of type BidState" using (bidState.single() is BidState)
            }
        }

        when (command.value) {
            is Commands.List -> {
                requireThat {
                    // Constraints on the shape of the Transaction
                    "No inputs should be consumed when creating and listing an item for bidding." using (tx.inputStates.isEmpty())
                    verifySingleOutputState()
                    val outputState = tx.outputsOfType<BidState>().single()
                    // Bid specific constraints
                    "Listing price should be positive." using (outputState.lastPrice > 0.SORDA)
                    "Issuer must be the lastSuccessfulBidder upon initial listing.." using (outputState.issuer == outputState.lastSuccessfulBidder)
                    // Constraints on the signers
                    // TODO: do we need any?
                }
            }
            is Commands.PlaceBid -> {
                requireThat {
                    // Constraints on the shape of the Transaction
                    verifySingleInputState()
                    verifySingleOutputState()

                    val inputState = tx.inputsOfType<BidState>().single()
                    val outputState = tx.outputsOfType<BidState>().single()

                    "Issuer can not place a Bid." using (inputState.issuer != outputState.lastSuccessfulBidder)

                    // Check that outputState is really derived from inputState
                    "Issuer must remain the same." using (inputState.issuer == outputState.issuer)
                    "Expiry must remain the same." using (inputState.expiry == outputState.expiry)
                    "Description must remain the same." using (inputState.description == outputState.description)
                    "Price must increase." using (inputState.lastPrice < outputState.lastPrice)

                    // Constraints on the signers.
                    val expectedSigners = listOf(outputState.issuer.owningKey, outputState.lastSuccessfulBidder.owningKey)
                    "There must be two signers." using (command.signers.toSet().size == 2)
                    "The issuer and lastSuccessfulBidder must be signers." using (command.signers.containsAll(expectedSigners))
                }
            }
            is Commands.CloseBid -> {
                // The Item gets Transferred to the latest successful bidder
                requireThat {
                    // Constraints on the shape of the Transaction
                    verifySingleInputState()

                    val inputBidState = tx.inputsOfType<BidState>().single()
                    val outputItemState = tx.outputsOfType<ItemState>().single()

                    // Check that Item gets updated according to Bid
                    "Item must be transferred to last successful bidder." using (inputBidState.lastSuccessfulBidder == outputItemState.owner)

                    // Constraints on the signers.
                    val expectedSigners = listOf(inputBidState.issuer.owningKey, inputBidState.lastSuccessfulBidder.owningKey)
                    "The issuer and lastSuccessfulBidder must be signers." using (command.signers.containsAll(expectedSigners))
                }
            }
        }
    }
}