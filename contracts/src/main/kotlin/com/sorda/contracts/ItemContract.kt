package com.sorda.contracts

import com.sorda.states.ItemState
import net.corda.core.contracts.*
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
    }

    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Transfer -> {
                //"There must be one output" using(tx.outputs.size == 1)
                val input = tx.inputsOfType<ItemState>().single()
                val output = tx.outputsOfType<ItemState>().single()
                "Only the owner can change in an item transfer" using
                        ( input.id == output.id && input.name == output.name)
            }
        }
    }
}