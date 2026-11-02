package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StatisticRecordDto(
    val ts: String,
    val dims: Map<String, String>,
    val vals: Map<String, Double>,
)

fun ResultRow.toStatisticRecordDto(): StatisticRecordDto? {
    val startTs = this[StatisticShortTerm.startTs] ?: return null
    val statisticId = this[StatisticMetaData.statisticId] ?: return null
    return StatisticRecordDto(
        ts = startTs.toString(),
        dims = mapOf("statistic_id" to statisticId),
        vals =
            listOf(
                "mean" to this[StatisticShortTerm.mean],
                "min" to this[StatisticShortTerm.min],
                "max" to this[StatisticShortTerm.max],
                "state" to this[StatisticShortTerm.state],
                "sum" to this[StatisticShortTerm.sum],
            ).mapNotNull { (key, value) -> value?.let { key to it } }.toMap(),
    )
}
