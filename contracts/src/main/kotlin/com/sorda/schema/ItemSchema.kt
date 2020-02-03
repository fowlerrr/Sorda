package com.sorda.schema


import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.*

object ItemContractsSchema

object ItemContractsSchemaV1 : MappedSchema (
        schemaFamily = ItemContractsSchema::class.java,
        version = 1,
        mappedTypes = listOf(PersistentItem::class.java)
) {

    @Entity
    @Table(name = "item_state")
    data class PersistentItem(
        @Column(name = "identifier", nullable = false)
        val id: UUID,
        @Column(name = "name", nullable = false)
        var name: String,
        @Column(name = "owner", nullable = false)
        var owner: Party?
    ) : PersistentState() {
        constructor () : this (UUID.randomUUID(), "", null)
    }

}