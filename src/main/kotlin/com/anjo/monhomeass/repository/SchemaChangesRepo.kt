package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.SchemaChangeDto
import com.anjo.monhomeass.model.dto.toSchemaChangeDto
import com.anjo.monhomeass.model.tables.SchemaChanges
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SchemaChangesRepo {

    fun getAll(): List<SchemaChangeDto> {
        return transaction {
            SchemaChanges.selectAll()
                    .map { it.toSchemaChangeDto() }
        }
    }
}