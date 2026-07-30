@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StatisticDto
import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.repository.StatisticMetaRepo
import com.anjo.monhomeass.repository.StatisticRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.time.Instant

fun Route.statisticMetaRoute(repo: StatisticMetaRepo) {

    route("/statistics-meta") {
        get {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }.describe {
            tag("Statistics")
            summary = "List trackable statistic entities"

            responses {
                HttpStatusCode.OK {
                    description = "List of statistic metadata entries."
                    schema = jsonSchema<List<StatisticMetaDto>>()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Recorder database unreachable or query failed."
                    schema = jsonSchema<ErrorDto>()
                }
            }
        }
    }
}

private fun parseInstant(value: String): Instant? {
    return value.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
            ?: runCatching { Instant.parse(value) }.getOrNull()
}

fun Route.statisticRoute(repo: StatisticRepo) {

    route("/statistics") {
        get("/{statisticId}") {
            val statisticId = call.parameters["statisticId"]!!
            val from = call.request.queryParameters["from"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'from' value"))
            }
            val to = call.request.queryParameters["to"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'to' value"))
            }

            val rows = repo.getLongTerm(statisticId, from, to)
            if (rows == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            } else {
                call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
            }
        }.describe {
            tag("Statistics")
            summary = "Long-term statistics for one entity"

            parameters {
                path("statisticId") {
                    schema = jsonSchema<String>()
                }
                query("from") {
                    required = false
                    schema = jsonSchema<Long>()
                }
                query("to") {
                    required = false
                    schema = jsonSchema<Long>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "Long-term statistic rows, oldest first."
                    schema = jsonSchema<List<StatisticDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "from/to present but unparseable."
                    schema = jsonSchema<ErrorDto>()
                }
                HttpStatusCode.NotFound {
                    description = "Unknown statisticId."
                    schema = jsonSchema<ErrorDto>()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Recorder database unreachable or query failed."
                    schema = jsonSchema<ErrorDto>()
                }
            }
        }
    }
}

fun Route.statisticShortTermRoute(repo: StatisticRepo) {

    route("/statistics") {
        get("/{statisticId}/short-term") {
            val statisticId = call.parameters["statisticId"]!!
            val from = call.request.queryParameters["from"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'from' value"))
            }
            val to = call.request.queryParameters["to"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'to' value"))
            }

            val rows = repo.getShortTerm(statisticId, from, to)
            if (rows == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            } else {
                call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
            }
        }.describe {
            tag("Statistics")
            summary = "Short-term (5-minute) statistics for one entity"

            parameters {
                path("statisticId") {
                    schema = jsonSchema<String>()
                }
                query("from") {
                    required = false
                    schema = jsonSchema<Long>()
                }
                query("to") {
                    required = false
                    schema = jsonSchema<Long>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "Short-term statistic rows, oldest first."
                    schema = jsonSchema<List<StatisticDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "from/to present but unparseable."
                    schema = jsonSchema<ErrorDto>()
                }
                HttpStatusCode.NotFound {
                    description = "Unknown statisticId."
                    schema = jsonSchema<ErrorDto>()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Recorder database unreachable or query failed."
                    schema = jsonSchema<ErrorDto>()
                }
            }
        }
    }
}
