package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.SensorSource
import com.anjo.monhomeass.model.dto.HaRegistries
import com.anjo.monhomeass.model.dto.SensorDto

private const val CLIMATE = "climate"
private const val AIR_QUALITY = "air-quality"
private const val ENERGY = "energy"
private const val UTILITY = "utility"
private const val BATTERY = "battery"
private const val LIGHT = "light"
private const val DIAGNOSTIC = "diagnostic"
private const val OTHER = "other"

private val CATEGORY_BY_DEVICE_CLASS: Map<String, String> =
    mapOf(
        CLIMATE to
            listOf(
                "temperature",
                "humidity",
                "pressure",
                "atmospheric_pressure",
                "moisture",
                "wind_speed",
                "precipitation",
                "precipitation_intensity",
            ),
        AIR_QUALITY to
            listOf(
                "carbon_dioxide",
                "carbon_monoxide",
                "pm1",
                "pm25",
                "pm10",
                "volatile_organic_compounds",
                "volatile_organic_compounds_parts",
                "aqi",
                "nitrogen_dioxide",
                "nitrogen_monoxide",
                "nitrous_oxide",
                "ozone",
                "sulphur_dioxide",
            ),
        ENERGY to
            listOf(
                "power",
                "energy",
                "energy_storage",
                "apparent_power",
                "reactive_power",
                "power_factor",
                "voltage",
                "current",
                "frequency",
            ),
        UTILITY to listOf("gas", "water", "volume", "volume_flow_rate"),
        BATTERY to listOf("battery"),
        LIGHT to listOf("illuminance", "irradiance"),
        DIAGNOSTIC to listOf("signal_strength", "data_rate", "data_size"),
    ).flatMap { (category, deviceClasses) -> deviceClasses.map { it to category } }.toMap()

private val DIAGNOSTIC_ENTITY_CATEGORIES = setOf("diagnostic", "config")

private val CATEGORY_BY_UNIT_CLASS: Map<String, String> =
    mapOf(
        "temperature" to CLIMATE,
        "pressure" to CLIMATE,
        "energy" to ENERGY,
        "power" to ENERGY,
        "electric_current" to ENERGY,
        "electric_potential" to ENERGY,
        "volume" to UTILITY,
    )

internal fun sensorCatalog(
    sources: List<SensorSource>,
    registries: HaRegistries?,
): List<SensorDto> {
    val entities = registries?.entities.orEmpty().associateBy { it.entityId }
    val devices = registries?.devices.orEmpty().associateBy { it.id }
    val areas = registries?.areas.orEmpty().associateBy { it.areaId }
    val floors = registries?.floors.orEmpty().associateBy { it.floorId }
    return sources
        .sortedBy { it.entityId }
        .map { source ->
            val sensor = source.toSensorDto()
            val entity = entities[source.entityId] ?: return@map sensor
            val device = entity.deviceId?.let { devices[it] }
            val area = (entity.areaId ?: device?.areaId)?.let { areas[it] }
            sensor.copy(
                name = registryName(entity, device, area, source.deviceClass) ?: sensor.name,
                area = area?.name,
                floor = area?.floorId?.let { floors[it]?.name },
                category = if (entity.entityCategory in DIAGNOSTIC_ENTITY_CATEGORIES) DIAGNOSTIC else sensor.category,
            )
        }.withUniqueNames()
}

private fun registryName(
    entity: HaRegistries.Entity,
    device: HaRegistries.Device?,
    area: HaRegistries.Area?,
    deviceClass: String?,
): String? =
    entity.name
        ?: device?.nameByUser?.let { deviceName ->
            entity.originalName?.takeIf { entity.hasEntityName }?.let { "$deviceName $it" } ?: deviceName
        }
        ?: area?.let { (deviceClass?.replace('_', ' ') ?: entity.originalName)?.let { what -> "${area.name} $what" } }

private fun SensorSource.toSensorDto(): SensorDto =
    SensorDto(
        entityId = entityId,
        name = friendlyName ?: metaName ?: prettyName(entityId),
        area = null,
        floor = null,
        category = category(),
        unit = unit ?: metaUnit,
    )

private fun SensorSource.category(): String =
    deviceClass?.let { CATEGORY_BY_DEVICE_CLASS[it] }
        ?: unitClass?.let { CATEGORY_BY_UNIT_CLASS[it] }
        ?: OTHER

private fun prettyName(entityId: String): String =
    entityId
        .split('.', ':', limit = 2)
        .last()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

private fun List<SensorDto>.withUniqueNames(): List<SensorDto> {
    val counts = mutableMapOf<String, Int>()
    return map { sensor ->
        val count = counts.getOrDefault(sensor.name, 0) + 1
        counts[sensor.name] = count
        if (count == 1) sensor else sensor.copy(name = "${sensor.name} $count")
    }
}
