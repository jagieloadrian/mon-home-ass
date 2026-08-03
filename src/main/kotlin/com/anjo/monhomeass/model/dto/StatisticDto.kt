package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticShortTem
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
        val state: Double?
)

fun ResultRow.toStatisticDto(): StatisticDto {
    return StatisticDto(
            startTs = this[Statistic.startTs]!!.epochSeconds,
            mean = this[Statistic.mean],
            meanWeight = this[Statistic.meanWeight],
            min = this[Statistic.min],
            max = this[Statistic.max],
            sum = this[Statistic.sum],
            state = this[Statistic.state]
    )
}

fun ResultRow.toStatisticDtoShortTerm(): StatisticDto {
    return StatisticDto(
            startTs = this[StatisticShortTem.startTs]!!.epochSeconds,
            mean = this[StatisticShortTem.mean],
            meanWeight = this[StatisticShortTem.meanWeight],
            min = this[StatisticShortTem.min],
            max = this[StatisticShortTem.max],
            sum = this[StatisticShortTem.sum],
            state = this[StatisticShortTem.state]
    )
}
