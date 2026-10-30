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
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.statisticMetaRoute(repo: StatisticMetaRepo) {
    route("/statistics-meta") {
        get {
            val search = call.request.queryParameters["search"]
            call.respond(
                repo.getAll(search),
                typeInfo = TypeInfo(ArrayList::class),
            )
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
            val summary =
                repo.getSummary(
                    call.parameters.getOrFail("statisticId"),
                    call.instantParam("from"),
                    call.instantParam("to"),
                ) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            call.respond(summary)
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

fun Route.statisticRoute(repo: StatisticRepo) {
    route("/statistics") {
        get("/{statisticId}") {
            val rows =
                repo.getLongTerm(
                    call.parameters.getOrFail("statisticId"),
                    call.instantParam("from"),
                    call.instantParam("to"),
                    call.limitParam(),
                    call.offsetParam(),
                ) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
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
            val rows =
                repo.getShortTerm(
                    call.parameters.getOrFail("statisticId"),
                    call.instantParam("from"),
                    call.instantParam("to"),
                    call.limitParam(),
                    call.offsetParam(),
                ) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown statisticId"))
            call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
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
