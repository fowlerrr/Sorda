package com.sorda.contracts

import com.sorda.states.ItemState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// Contract and state.
class ItemContract: Contract {
    companion object {
        @JvmStatic
        val ID = "com.sorda.contracts.ItemContract"
    }

    // Command.
    interface Commands : CommandData {
        class Transfer : TypeOnlyCommandData(), Commands
        class Issue : TypeOnlyCommandData(), Commands
    }

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Transfer -> {
//                "There must be one output" using(tx.outputs.size == 1)
                val input = tx.inputsOfType<ItemState>().single()
                val output = tx.outputsOfType<ItemState>().single()
                "Only the owner can change in an item transfer" using
                        ( input.linearId == output.linearId && input.name == output.name)
            }
            is Commands.Issue -> {
                // Shape checks
                "No input ItemState must be consumed." using (tx.inputs.isEmpty())
                val output = tx.outputsOfType<ItemState>()
                "Exactly one output ItemState must be created." using (output.size == 1)
            }
        }
    }
}