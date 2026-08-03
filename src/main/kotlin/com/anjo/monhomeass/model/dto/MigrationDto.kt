package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.MigrationChanges
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class MigrationDto(val id: String, val version: Int)

fun ResultRow.toMigrationDto(): MigrationDto {
    return MigrationDto(
            id = this[MigrationChanges.id],
            version = this[MigrationChanges.version])
}