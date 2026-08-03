package com.anjo.monhomeass.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticSummaryDto(
        val statisticId: String,
        val from: String?,
        val to: String?,
        val min: Double?,
        val max: Double?,
        val avg: Double?,
        val sum: Double?
)
