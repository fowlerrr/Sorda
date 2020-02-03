package com.sorda.contracts

import com.sorda.states.BidState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import utils.SORDA

// Contract and state.
class SordaContract: Contract {
    companion object {
        @JvmStatic
        val ID = "com.sorda.contracts.SordaContract"
    }

    // Command.
    interface Commands : CommandData {
        class Start : TypeOnlyCommandData(), Commands
        class End : TypeOnlyCommandData(), Commands
    }

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Start -> {
                //"There must be one output" using(tx.outputs.size == 1)
                //val state = tx.outputsOfType<SordaState>().single()
            }
            is Commands.End -> {
                //val state = tx.outputsOfType<SordaState>().single()
                //"Make sure participants are different" using (state.participants[0] != state.participants[1])
                //"End time cannot be less than start time" using (state.startTime <= state.endTime)
            }
        }
    }
}

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
                "This contract supports only a single input state" using (tx.inputStates.size == 1)
                "There should be one input state of type BidState" using (tx.inputStates.single() is BidState)
            }
        }

        fun verifySingleOutputState() {
            requireThat {
                "This contract supports only a single output state" using (tx.outputStates.size == 1)
                "There should be one output state of type BidState" using (tx.outputStates.single() is BidState)
            }
        }

        when (command.value) {
            is Commands.List -> {
                requireThat {
                    // Constraints on the shape of the Transaction
                    "No inputs should be consumed when creating and listing an item for bidding." using (tx.inputStates.isEmpty())

                    verifySingleOutputState()
                    val outputState = tx.outputStates.single() as BidState

                    // Bid specific constraints
                    "Listing price should be positive." using (outputState.lastPrice > 0.SORDA)

                    // Constraints on the signers
                    // TODO: do we need any?
                }
            }
            is Commands.PlaceBid -> {
                requireThat {
                    // Constraints on the shape of the Transaction
                    verifySingleInputState()
                    verifySingleOutputState()

                    val inputState = tx.inputStates.single() as BidState
                    val outputState = tx.outputStates.single() as BidState

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

            }
        }
    }
}