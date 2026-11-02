package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.SchemaChangeDto
import com.anjo.monhomeass.model.dto.toSchemaChangeDto
import com.anjo.monhomeass.model.tables.SchemaChanges
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SchemaChangesRepo {
    fun getAll(): List<SchemaChangeDto> =
        transaction {
            SchemaChanges
                .selectAll()
                .map { it.toSchemaChangeDto() }
        }
}
