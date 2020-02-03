package com.sorda.states


import com.sorda.contracts.ItemContract
import com.sorda.schema.ItemContractsSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable


@CordaSerializable
@BelongsToContract(ItemContract::class)
data class ItemState (
    val owner: Party,
    val name: String,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(owner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (schema is ItemContractsSchemaV1) {
            return ItemContractsSchemaV1.PersistentItem(
                id = linearId.id,
                name = name,
                owner = owner
            )
        } else {
            throw IllegalStateException("Cannot construct instance of ${this.javaClass} from Schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(ItemContractsSchemaV1)
    }
}