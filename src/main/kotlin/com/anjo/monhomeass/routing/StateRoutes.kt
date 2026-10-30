@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StateDto
import com.anjo.monhomeass.repository.StateRepo
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

fun Route.stateRoute(repo: StateRepo) {
    route("/states") {
        get("/{entityId}") {
            val state =
                repo.getLatest(call.parameters.getOrFail("entityId"))
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown entityId"))
            call.respond(state)
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
            val rows =
                repo.getHistory(
                    call.parameters.getOrFail("entityId"),
                    call.instantParam("from"),
                    call.instantParam("to"),
                    call.limitParam(),
                    call.offsetParam(),
                ) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorDto("Unknown entityId"))
            call.respond(rows, typeInfo = TypeInfo(ArrayList::class))
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
