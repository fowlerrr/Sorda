package com.sorda.schema


import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.*

object SordaContractsSchema

object SordaContractsSchemaV1 : MappedSchema (
        schemaFamily = SordaContractsSchema::class.java,
        version = 1,
        mappedTypes = listOf(PersistentSorda::class.java)
) {
    @Entity
    @Table(name = "sorda_state")
    data class PersistentSorda(
        @Column(name = "identifier", nullable = false)
        val id: UUID,
        @Column(name = "value", nullable = false)
        var amount: Long
    ) : PersistentState() {
            constructor () : this (UUID.randomUUID(), 0)
    }

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