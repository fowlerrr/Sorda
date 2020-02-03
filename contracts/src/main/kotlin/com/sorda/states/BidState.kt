package com.sorda.states


import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

import java.time.Instant

@BelongsToContract(BidContract::class)
class BidState (
        val description: String,
        val issuer: Party,
        val lastSuccessfulBidder: Party,
        val lastPrice: Amount<TokenType>,
        val expiry: Instant,
        val itemLinearId: UniqueIdentifier,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : SchedulableState, LinearState {

    override val participants: List<AbstractParty>
        get() = listOf<Party>(issuer, lastSuccessfulBidder)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create(TransferItemFlow::class.java, lastSuccessfulBidder, itemLinearId, linearId), expiry)
    }
}


