package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.SchemaChanges
import com.anjo.monhomeass.utils.toKotlinxDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class SchemaChangeDto(
        val id: Int,
        val version: Int?,
        val changedAt: LocalDateTime,
)

fun ResultRow.toSchemaChangeDto(): SchemaChangeDto {
    return SchemaChangeDto(
            id = this[SchemaChanges.id],
            version = this[SchemaChanges.version],
            changedAt = this[SchemaChanges.changedAt].toKotlinxDateTime()
    )
}