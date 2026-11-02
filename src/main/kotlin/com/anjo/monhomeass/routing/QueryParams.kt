package com.anjo.monhomeass.routing

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import kotlin.time.Instant

internal fun ApplicationCall.instantParam(name: String): Instant? = queryParam(name, ::parseInstant)

internal fun ApplicationCall.limitParam(): Int? = queryParam("limit") { it.toIntOrNull()?.takeIf { v -> v >= 0 } }

internal fun ApplicationCall.offsetParam(): Long? = queryParam("offset") { it.toLongOrNull()?.takeIf { v -> v >= 0 } }

private inline fun <T : Any> ApplicationCall.queryParam(
    name: String,
    parse: (String) -> T?,
): T? = request.queryParameters[name]?.let { parse(it) ?: throw BadRequestException("Invalid '$name' value") }

private fun parseInstant(value: String): Instant? =
    value.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
        ?: runCatching { Instant.parse(value) }.getOrNull()
