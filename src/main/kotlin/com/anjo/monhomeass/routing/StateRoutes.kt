@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StateDto
import com.anjo.monhomeass.repository.StateRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.stateRoute(repo: StateRepo) {

    route("/states") {
        get("/{entityId}") {
            val entityId = call.parameters["entityId"]!!

            val state = repo.getLatest(entityId)
            if (state == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown entityId"))
            } else {
                call.respond(state)
            }
        }.describe {
            tag("States")
            summary = "Latest recorded state for one entity"

            parameters {
                path("entityId") {
                    schema = jsonSchema<String>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "The most recent state row for this entity."
                    schema = jsonSchema<StateDto>()
                }
                HttpStatusCode.NotFound {
                    description = "Unknown entityId, or entity has no recorded state rows."
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

fun Route.stateHistoryRoute(repo: StateRepo) {

    route("/states") {
        get("/{entityId}/history") {
            val entityId = call.parameters["entityId"]!!
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

            val rows = repo.getHistory(entityId, from, to, limit, offset)
            if (rows == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown entityId"))
            } else {
                call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
            }
        }.describe {
            tag("States")
            summary = "State history for one entity"

            parameters {
                path("entityId") {
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
                    description = "State rows, oldest first, optionally paginated. Empty array if entity known but has no rows in range."
                    schema = jsonSchema<List<StateDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "from/to/limit/offset present but unparseable or negative."
                    schema = jsonSchema<ErrorDto>()
                }
                HttpStatusCode.NotFound {
                    description = "Unknown entityId."
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
