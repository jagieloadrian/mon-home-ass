package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.MigrationDto
import com.anjo.monhomeass.model.dto.toMigrationDto
import com.anjo.monhomeass.model.tables.MigrationChanges
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MigrationRepo {

    fun getAll(): List<MigrationDto> {
        return transaction {
            MigrationChanges.selectAll()
                    .map { it.toMigrationDto() }
        }
    }
}