package com.sorda.flows.tokens

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import utils.SordaTokenType

/**
 * This flow leverages Tokens-SDK in order to issue SORDA Tokens to an existing account.
 *
 * @property accountName the Name of the account
 * @property quantity the quantity of the SORDA Tokens to be issued
 *
 */

@StartableByRPC
@InitiatingFlow
class IssueSordaTokens (
        private val quantity: Double
) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = IssueSordaTokens.tracker()

    companion object {
        object ISSUE : ProgressTracker.Step("Issue Token")
        object COMPLETE : ProgressTracker.Step("Complete Token Issuance") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
        fun tracker() = ProgressTracker(ISSUE, COMPLETE)
    }

    @Suspendable
    override fun call() : SignedTransaction {
        val tokens = quantity of SordaTokenType issuedBy ourIdentity heldBy ourIdentity

        return subFlow(IssueTokens(listOf(tokens), emptyList()))

    }
}