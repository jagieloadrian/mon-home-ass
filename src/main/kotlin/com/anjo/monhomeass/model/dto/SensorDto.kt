package com.anjo.monhomeass.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SensorDto(
    val entityId: String,
    val name: String,
    val area: String?,
    val floor: String?,
    val category: String,
    val unit: String?,
)
