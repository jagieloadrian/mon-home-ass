package com.anjo.monhomeass.publish

import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.model.dto.StatisticRecordDto
import com.anjo.monhomeass.repository.SensorRepo
import com.anjo.monhomeass.repository.StatisticRepo
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class StatisticPublisher(
    private val repo: StatisticRepo,
    private val sensors: SensorRepo,
    private val sink: Sink,
    private val since: Instant,
    private val batchSize: Int = 500,
) {
    private var lastId = 0L

    suspend fun publishNew(): Int {
        val catalog = sensors.getAll().associateBy { it.entityId }
        var published = 0
        while (true) {
            val batch = repo.getShortTermAfter(lastId, since, batchSize)
            batch.forEach { (id, record) ->
                sink.send(Json.encodeToString(record.withSensorDims(catalog)).toByteArray())
                lastId = id
                published++
            }
            if (batch.size < batchSize) return published
        }
    }
}

private fun StatisticRecordDto.withSensorDims(catalog: Map<String, SensorDto>): StatisticRecordDto =
    dims["statistic_id"]?.let { catalog[it] }?.let { copy(dims = dims + it.dims()) } ?: this

private fun SensorDto.dims(): Map<String, String> =
    listOf("name" to name, "area" to area, "floor" to floor, "category" to category, "unit" to unit)
        .mapNotNull { (key, value) -> value?.let { key to it } }
        .toMap()
