package com.sorda.states


import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import com.sorda.contracts.ExpiryContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

import java.time.Instant

@BelongsToContract(ExpiryContract::class)
data class ExpiryState (
        val description: String,
        val issuer: Party,
        val expiry: Instant,
        val itemLinearId: UniqueIdentifier,
        val bidLinearId: UniqueIdentifier,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : SchedulableState, LinearState {

    override val participants: List<AbstractParty>
        get() = listOf<Party>(issuer)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create(TransferItemFlow::class.java,
                itemLinearId,
                bidLinearId
                ), expiry)
    }
}