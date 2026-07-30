package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.Statistic
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StatisticDto(
        val startTs: Long,
        val mean: Double?,
        val min: Double?,
        val max: Double?,
        val sum: Double?,
        val state: Double?
)

fun ResultRow.toStatisticDto(): StatisticDto {
    return StatisticDto(
            startTs = this[Statistic.startTs]!!.epochSeconds,
            mean = this[Statistic.mean],
            min = this[Statistic.min],
            max = this[Statistic.max],
            sum = this[Statistic.sum],
            state = this[Statistic.state]
    )
}
