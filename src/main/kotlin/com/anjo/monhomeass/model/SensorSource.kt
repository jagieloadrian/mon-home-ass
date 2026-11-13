package com.anjo.monhomeass.model

import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StatisticMetaData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jetbrains.exposed.v1.core.ResultRow

data class SensorSource(
    val entityId: String,
    val friendlyName: String?,
    val deviceClass: String?,
    val unit: String?,
    val metaName: String?,
    val metaUnit: String?,
    val unitClass: String?,
)

fun ResultRow.toSensorSource(): SensorSource? {
    val entityId = this[StatisticMetaData.statisticId] ?: return null
    val attributes = this[StateAttributes.sharedAttributes]?.let { Json.parseToJsonElement(it) as? JsonObject }.orEmpty()

    fun attribute(key: String) = (attributes[key] as? JsonPrimitive)?.contentOrNull
    return SensorSource(
        entityId = entityId,
        friendlyName = attribute("friendly_name"),
        deviceClass = attribute("device_class"),
        unit = attribute("unit_of_measurement"),
        metaName = this[StatisticMetaData.name],
        metaUnit = this[StatisticMetaData.unitOfMeasurement],
        unitClass = this[StatisticMetaData.unitClass],
    )
}
