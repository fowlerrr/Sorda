package com.sorda.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

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