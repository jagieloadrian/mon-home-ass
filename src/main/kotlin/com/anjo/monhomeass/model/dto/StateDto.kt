package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StateDto(
        val entityId: String,
        val state: String?,
        val attributes: String?,
        val lastChangedTs: Long?,
        val lastUpdatedTs: Long?
)

fun ResultRow.toStateDto(): StateDto {
    return StateDto(
            entityId = this[StateMetadata.entityId]!!,
            state = this[State.state],
            attributes = this.getOrNull(StateAttributes.sharedAttributes),
            lastChangedTs = this[State.lastModifiedTs]?.epochSeconds,
            lastUpdatedTs = this[State.lastUpdatedTs]?.epochSeconds
    )
}
