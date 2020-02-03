package com.sorda.states


import com.sorda.contracts.ItemContract
import com.sorda.schema.SordaContractsSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


@BelongsToContract(ItemContract::class)
data class ItemState (
    val owner: Party,
    val name: String,
    val id: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(owner)

    // TODO: Check if we double-generating this unnecessarily
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