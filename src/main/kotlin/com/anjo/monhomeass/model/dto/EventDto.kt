package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.Event
import com.anjo.monhomeass.model.tables.EventData
import com.anjo.monhomeass.model.tables.EventType
import com.anjo.monhomeass.utils.toKotlinxDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class EventDto(
        val id: Int,
        val type: String?,
        val data: String?,
        val origin: String?,
        val originIdx: Int?,
        val timestamp: LocalDateTime?,
        val timestampTs: Instant?,
        val contextId: String?,
        val contextUserId: String?,
        val contextParentId: String?,
        val dataContent: DataContentDto?,
        val contextIdBinary: String?,
        val contextUserIdBinary: String?,
        val contextParentIdBinary: String?,
        val eventType: EventTypeDto?
)

@Serializable
data class DataContentDto(
        val id: Int,
        val hash: Long?,
        val sharedData: String?,
)

@Serializable
data class EventTypeDto(
        val id: Int,
        val type: String?
)

@OptIn(ExperimentalStdlibApi::class)
fun ResultRow.toEventDto(): EventDto {
    val eventType = generateEventType(this)
    val eventData = generateEventData(this)

    return EventDto(
            id = this[Event.id],
            type = this[Event.type],
            data = this[Event.data],
            origin = this[Event.origin],
            originIdx = this[Event.originIdx],
            timestamp = this[Event.timestamp]?.toKotlinxDateTime(),
            timestampTs = this[Event.timestampTs],
            contextId = this[Event.contextId],
            contextUserId = this[Event.contextUserId],
            contextParentId = this[Event.contextParentId],
            dataContent = eventData,
            contextIdBinary = this[Event.contextIdBinary]?.toHexString(),
            contextUserIdBinary = this[Event.contextUserIdBinary]?.toHexString(),
            contextParentIdBinary = this[Event.contextParentIdBinary]?.toHexString(),
            eventType = eventType
    )
}

fun generateEventType(resultRow: ResultRow): EventTypeDto? {
    return resultRow[Event.eventTypeId]?.let {
        EventTypeDto(
                id = resultRow[EventType.id],
                type = resultRow[EventType.type],
        )
    }
}

fun generateEventData(resultRow: ResultRow): DataContentDto? {
    return resultRow[Event.dataId]?.let {
        DataContentDto(
                id = resultRow[EventData.id],
                hash = resultRow[EventData.hash],
                sharedData = resultRow[EventData.sharedData]
        )
    }
}
