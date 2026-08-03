@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StatisticDto
import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.model.dto.StatisticSummaryDto
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
            val search = call.request.queryParameters["search"]
            call.respond(repo.getAll(search),
                    typeInfo = TypeInfo(ArrayList::class))
        }.describe {
            tag("Statistics")
            summary = "List trackable statistic entities"

            parameters {
                query("search") {
                    required = false
                    schema = jsonSchema<String>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "List of statistic metadata entries, optionally filtered by search."
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

fun Route.statisticSummaryRoute(repo: StatisticRepo) {

    route("/statistics") {
        get("/{statisticId}/summary") {
            val statisticId = call.parameters["statisticId"]!!
            val from = call.request.queryParameters["from"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'from' value"))
            }
            val to = call.request.queryParameters["to"]?.let {
                parseInstant(it) ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'to' value"))
            }

            val summary = repo.getSummary(statisticId, from, to)
            if (summary == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            } else {
                call.respond(summary)
            }
        }.describe {
            tag("Statistics")
            summary = "Aggregated min/max/avg/sum over a date range"

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
                    description = "Aggregate summary for the range."
                    schema = jsonSchema<StatisticSummaryDto>()
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

internal fun parseInstant(value: String): Instant? {
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

            val limit = call.request.queryParameters["limit"]?.let {
                val v = it.toIntOrNull()
                if (v == null || v < 0) return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'limit' value"))
                v
            }
            val offset = call.request.queryParameters["offset"]?.let {
                val v = it.toLongOrNull()
                if (v == null || v < 0) return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'offset' value"))
                v
            }

            val rows = repo.getLongTerm(statisticId, from, to, limit, offset)
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
                query("limit") {
                    required = false
                    schema = jsonSchema<Int>()
                }
                query("offset") {
                    required = false
                    schema = jsonSchema<Long>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "Long-term statistic rows, oldest first, optionally paginated."
                    schema = jsonSchema<List<StatisticDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "from/to/limit/offset present but unparseable or negative."
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
            val limit = call.request.queryParameters["limit"]?.let {
                val v = it.toIntOrNull()
                if (v == null || v < 0) return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'limit' value"))
                v
            }
            val offset = call.request.queryParameters["offset"]?.let {
                val v = it.toLongOrNull()
                if (v == null || v < 0) return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'offset' value"))
                v
            }

            val rows = repo.getShortTerm(statisticId, from, to, limit, offset)
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
                query("limit") {
                    required = false
                    schema = jsonSchema<Int>()
                }
                query("offset") {
                    required = false
                    schema = jsonSchema<Long>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "Short-term statistic rows, oldest first, optionally paginated."
                    schema = jsonSchema<List<StatisticDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "from/to/limit/offset present but unparseable or negative."
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
