package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTem
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

// Native record of the StatisticsService ingest contract
// (StatisticsService/.docs/Collector_Apps.md): {ts, dims, vals}, nulls dropped.
@Serializable
data class StatisticRecordDto(
        val ts: String,
        val dims: Map<String, String>,
        val vals: Map<String, Double>
)

fun ResultRow.toStatisticRecordDto(): StatisticRecordDto {
    return StatisticRecordDto(
            ts = this[StatisticShortTem.startTs]!!.toString(),
            dims = mapOf("statistic_id" to this[StatisticMetaData.statisticId]!!),
            vals = listOf(
                    "mean" to this[StatisticShortTem.mean],
                    "min" to this[StatisticShortTem.min],
                    "max" to this[StatisticShortTem.max],
                    "state" to this[StatisticShortTem.state],
                    "sum" to this[StatisticShortTem.sum]
            ).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    )
}
