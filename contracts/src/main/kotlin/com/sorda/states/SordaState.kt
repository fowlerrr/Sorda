package com.sorda.states


import co.paralleluniverse.fibers.Suspendable
import com.sorda.contracts.SordaContract
import com.sorda.schema.SordaContractsSchemaV1
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.BidContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.flows.SchedulableFlow
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import utils.SORDA
import java.time.Instant


@BelongsToContract(SordaContract::class)
data class SordaState (
        val party: Party,
        val id: UniqueIdentifier,
        val amount: Amount<TokenType> = 0.SORDA
) : LinearState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is SordaContractsSchemaV1) {
            return SordaContractsSchemaV1.PersistentSorda(
                    id = id.id,
                    amount = amount.quantity)
        } else {
            throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(SordaContractsSchemaV1)
    }

    override val linearId = id

    override val participants: List<AbstractParty> get() = listOf(party)

    override fun toString() =
        " linearId " + linearId +
        " amount = $amount"
}

@BelongsToContract(BidContract::class)
class BidState (
        val description: String,
        val issuer: Party,
        val lastSuccessfulBidder: Party,
        val lastPrice: Amount<TokenType>,
        val expiry: Instant) : SchedulableState {

    override val participants: List<AbstractParty>
        get() = listOf<Party>(issuer, lastSuccessfulBidder)
    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        return ScheduledActivity(flowLogicRefFactory.create(AuctionEndFlow::class.java), expiry)
    }
}

@SchedulableFlow
class AuctionEndFlow : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

    }
}