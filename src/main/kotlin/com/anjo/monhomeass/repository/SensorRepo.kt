package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.SensorSource
import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.toSensorSource
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private const val CACHE_KEY = "sensors:all"

class SensorRepo(
    private val cache: RedisCache,
) {
    fun getAll(
        area: String? = null,
        category: String? = null,
    ): List<SensorDto> =
        cache
            .getOrPut(CACHE_KEY) { sensorCatalog(sources()) }
            .orEmpty()
            .filter { (area == null || it.area == area) && (category == null || it.category == category) }

    private fun sources(): List<SensorSource> =
        transaction {
            val latestStateId = State.id.max().alias("latest_state_id")
            val latest =
                State
                    .select(State.metadataId, latestStateId)
                    .groupBy(State.metadataId)
                    .alias("latest")
            StatisticMetaData
                .join(StateMetadata, JoinType.LEFT, StatisticMetaData.statisticId, StateMetadata.entityId)
                .join(latest, JoinType.LEFT, StateMetadata.id, latest[State.metadataId])
                .join(State, JoinType.LEFT, latest[latestStateId], State.id)
                .join(StateAttributes, JoinType.LEFT, State.attributesId, StateAttributes.id)
                .select(
                    StatisticMetaData.statisticId,
                    StatisticMetaData.name,
                    StatisticMetaData.unitOfMeasurement,
                    StatisticMetaData.unitClass,
                    StateAttributes.sharedAttributes,
                ).mapNotNull { it.toSensorSource() }
        }
}
