package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StateMetadata
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class EntityDto(
        val id: Long,
        val entityId: String?
)

fun ResultRow.toEntityDto(): EntityDto {
    return EntityDto(
            id = this[StateMetadata.id],
            entityId = this[StateMetadata.entityId]
    )
}
