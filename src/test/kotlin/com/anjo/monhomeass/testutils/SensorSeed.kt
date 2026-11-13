package com.anjo.monhomeass.testutils

import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val SEEDED_SENSORS =
    listOf(
        SensorDto("opower:elec_cost", "Elec cost", null, null, "other", null),
        SensorDto("sensor.0x00158d0001a2b3c4_power", "0x00158d0001a2b3c4 power", null, null, "energy", "W"),
        SensorDto("sensor.kitchen_co2", "Kitchen CO2", null, null, "air-quality", "ppm"),
        SensorDto("sensor.living_room_temperature", "Living room temperature", null, null, "climate", "°C"),
        SensorDto("sensor.living_room_temperature_2", "Living room temperature 2", null, null, "climate", "°C"),
    )

inline fun <T> withSeededSensors(
    database: Database,
    block: () -> T,
): T {
    transaction(database) { seedSensors() }
    val previous = TransactionManager.defaultDatabase
    TransactionManager.defaultDatabase = database
    return try {
        block()
    } finally {
        TransactionManager.defaultDatabase = previous
        transaction(database) { dropSensorTables() }
    }
}

fun dropSensorTables() {
    listOf(State, StateAttributes, StateMetadata, StatisticShortTerm, Statistic, StatisticMetaData).forEach {
        SchemaUtils.drop(it)
    }
}

fun seedSensors() {
    dropSensorTables()
    SchemaUtils.create(StatisticMetaData, StateMetadata, StateAttributes)
    TransactionManager.current().exec("CREATE TABLE states (state_id BIGINT PRIMARY KEY, metadata_id BIGINT, attributes_id BIGINT)")
    statistic(1, "sensor.living_room_temperature", metaName = null, metaUnit = "°F", metaUnitClass = "temperature")
    statistic(2, "sensor.0x00158d0001a2b3c4_power", metaName = null, metaUnit = "W", metaUnitClass = "power")
    statistic(3, "sensor.kitchen_co2", metaName = "Kitchen CO2", metaUnit = "ppm", metaUnitClass = null)
    statistic(4, "opower:elec_cost", metaName = null, metaUnit = null, metaUnitClass = null)
    statistic(5, "sensor.living_room_temperature_2", metaName = null, metaUnit = "°C", metaUnitClass = "temperature")
    state(
        1,
        "sensor.living_room_temperature",
        """{"friendly_name":"Old name","device_class":"temperature","unit_of_measurement":"°F"}""",
        """{"friendly_name":"Living room temperature","device_class":"temperature","unit_of_measurement":"°C"}""",
    )
    state(2, "sensor.kitchen_co2", """{"device_class":"carbon_dioxide","unit_of_measurement":"ppm"}""")
    state(
        3,
        "sensor.living_room_temperature_2",
        """{"friendly_name":"Living room temperature","device_class":"temperature","unit_of_measurement":"°C"}""",
    )
    state(4, "sensor.not_a_statistic", """{"friendly_name":"Not a statistic"}""")
}

private fun statistic(
    rowId: Long,
    entity: String,
    metaName: String?,
    metaUnit: String?,
    metaUnitClass: String?,
) {
    StatisticMetaData.insert {
        it[id] = rowId
        it[statisticId] = entity
        it[name] = metaName
        it[unitOfMeasurement] = metaUnit
        it[unitClass] = metaUnitClass
        it[meanType] = 0
    }
}

private fun state(
    metadataRowId: Long,
    entity: String,
    vararg attributeHistory: String,
) {
    StateMetadata.insert {
        it[id] = metadataRowId
        it[entityId] = entity
    }
    attributeHistory.forEachIndexed { index, attributes ->
        val rowId = metadataRowId * 10 + index
        StateAttributes.insert {
            it[id] = rowId
            it[sharedAttributes] = attributes
        }
        State.insert {
            it[id] = rowId
            it[metadataId] = metadataRowId
            it[attributesId] = rowId
        }
    }
}
