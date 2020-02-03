package com.sorda.states


import com.sorda.contracts.SordaContract
import com.sorda.schema.SordaContractsSchemaV1
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import utils.SORDA


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