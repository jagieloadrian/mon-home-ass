package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StatisticTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StatisticDto(
    val startTs: Long,
    val mean: Double?,
    val meanWeight: Double?,
    val min: Double?,
    val max: Double?,
    val sum: Double?,
    val state: Double?,
)

fun ResultRow.toStatisticDto(table: StatisticTable): StatisticDto? =
    this[table.startTs]?.let { startTs ->
        StatisticDto(
            startTs = startTs.epochSeconds,
            mean = this[table.mean],
            meanWeight = this[table.meanWeight],
            min = this[table.min],
            max = this[table.max],
            sum = this[table.sum],
            state = this[table.state],
        )
    }
