package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StatisticRun
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StatisticRunDto(
        val runId: Long,
        val startedAtTs: Long
)

fun ResultRow.toStatisticRunDto(): StatisticRunDto {
    return StatisticRunDto(
            runId = this[StatisticRun.id],
            startedAtTs = this[StatisticRun.startedAt].toInstant().epochSecond
    )
}
