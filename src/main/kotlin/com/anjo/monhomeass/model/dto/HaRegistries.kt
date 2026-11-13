package com.anjo.monhomeass.model.dto

import kotlinx.serialization.Serializable

data class HaRegistries(
    val entities: List<Entity>,
    val devices: List<Device>,
    val areas: List<Area>,
    val floors: List<Floor>,
) {
    @Serializable
    data class Entity(
        val entityId: String,
        val name: String? = null,
        val originalName: String? = null,
        val hasEntityName: Boolean = false,
        val deviceId: String? = null,
        val areaId: String? = null,
        val entityCategory: String? = null,
    )

    @Serializable
    data class Device(
        val id: String,
        val nameByUser: String? = null,
        val areaId: String? = null,
    )

    @Serializable
    data class Area(
        val areaId: String,
        val name: String,
        val floorId: String? = null,
    )

    @Serializable
    data class Floor(
        val floorId: String,
        val name: String,
    )
}
