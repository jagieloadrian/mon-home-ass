package com.anjo.monhomeass.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val message: String,
)
