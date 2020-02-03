package com.sorda.states


import com.sorda.contracts.SordaContract
import com.sorda.schema.SordaContractsSchemaV1
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.contracts.ItemContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.notary.validateTimeWindow
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import utils.SORDA


@BelongsToContract(ItemContract::class)
data class ItemState (
    val owner: Party,
    val name: String,
    val id: UniqueIdentifier,
    override val participants: List<AbstractParty> = listOf(owner)
) : LinearState, QueryableState {

    override val linearId = id

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is SordaContractsSchemaV1) {
            return SordaContractsSchemaV1.PersistentItem(
                id = id.id,
                name = name,
                owner = owner
            )
        } else {
            throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(SordaContractsSchemaV1)
    }
}