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

class ExpiryContract: Contract {
    companion object {
        @JvmStatic
        val ID = "com.sorda.contracts.ExpiryContract"
    }

    interface Commands : CommandData {
        class List : TypeOnlyCommandData(), Commands
        class CloseBid : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.List -> {
                requireThat {

                }
            }
            is Commands.CloseBid -> {
                // The Item gets Transferred to the latest successful bidder
                requireThat {

                }
            }
        }
    }
}