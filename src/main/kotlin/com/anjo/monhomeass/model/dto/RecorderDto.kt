package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.Recorder
import com.anjo.monhomeass.utils.toKotlinxDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class RecorderDto(
        val id: Int,
        val startTime: LocalDateTime,
        val endTime: LocalDateTime?,
        val isClosedIncorrect: Boolean,
        val createdAt: LocalDateTime
)

fun ResultRow.toRecorderDto(): RecorderDto {
    return RecorderDto(
            id = this[Recorder.runId],
            startTime = this[Recorder.startTime].toKotlinxDateTime(),
            endTime = this[Recorder.endTime]?.toKotlinxDateTime(),
            isClosedIncorrect = this[Recorder.isClosedIncorrect],
            createdAt = this[Recorder.createdAt].toKotlinxDateTime()
    )
}
