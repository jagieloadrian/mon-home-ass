package com.anjo.monhomeass.model.dto

import com.anjo.monhomeass.model.tables.StatisticMetaData
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow

@Serializable
data class StatisticMetaDto(
        val id: Int,
        val statisticId: String?,
        val name: String?,
        val unitOfMeasurement: String?,
        val source: String?,
        val hasMean: Boolean?,
        val hasSum: Boolean?
)

fun ResultRow.toStatisticMetaDto(): StatisticMetaDto {
    return StatisticMetaDto(
            id = this[StatisticMetaData.id],
            statisticId = this[StatisticMetaData.statisticId],
            name = this[StatisticMetaData.name],
            unitOfMeasurement = this[StatisticMetaData.unitOfMeasurement],
            source = this[StatisticMetaData.sourceOfData],
            hasMean = this[StatisticMetaData.hasMean],
            hasSum = this[StatisticMetaData.hasSum]
    )
}
