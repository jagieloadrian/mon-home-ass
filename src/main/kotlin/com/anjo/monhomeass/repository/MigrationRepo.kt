package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.MigrationDto
import com.anjo.monhomeass.model.dto.toMigrationDto
import com.anjo.monhomeass.model.tables.MigrationChanges
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MigrationRepo {
    fun getAll(): List<MigrationDto> =
        transaction {
            MigrationChanges
                .selectAll()
                .map { it.toMigrationDto() }
        }
}
