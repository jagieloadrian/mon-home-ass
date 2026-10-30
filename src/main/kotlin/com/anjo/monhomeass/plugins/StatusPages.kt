package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.model.dto.ErrorDto
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

fun Application.configureStatusPages() {
    // ponytail: swaggerUI's OpenApiDocSource.Routing re-runs the application module
    // to enumerate routes, so this can be called twice per real start — guard it.
    if (pluginOrNull(StatusPages) != null) return
    install(StatusPages) {
        // Invalid query params and missing path params (getOrFail) end up here.
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorDto(cause.message ?: "Bad request"))
        }
        exception<ExposedSQLException> { call, cause ->
            logger.error(cause) { "Recorder database query failed" }
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorDto("Recorder database unavailable"))
        }
        exception<SQLException> { call, cause ->
            logger.error(cause) { "Recorder database unreachable" }
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorDto("Recorder database unavailable"))
        }
    }
}
