package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.homeassistant.HaRegistryClient
import com.anjo.monhomeass.model.SensorSource
import com.anjo.monhomeass.model.dto.HaRegistries
import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.toSensorSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.coroutines.cancellation.CancellationException

private const val CACHE_KEY = "sensors:all"

private val logger = KotlinLogging.logger {}

class SensorRepo(
    private val cache: RedisCache,
    private val registryClient: HaRegistryClient? = null,
) {
    suspend fun getAll(
        area: String? = null,
        category: String? = null,
    ): List<SensorDto> =
        cache
            .getOrPut(CACHE_KEY) { sensorCatalog(sources(), registries()) }
            .orEmpty()
            .filter { (area == null || it.area == area) && (category == null || it.category == category) }

    private suspend fun registries(): HaRegistries? {
        val client = registryClient ?: return null
        return try {
            client.fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Home Assistant registries unavailable, sensor catalog falls back to the recorder DB" }
            null
        }
    }

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
