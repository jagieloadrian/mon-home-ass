@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StatisticRunDto
import com.anjo.monhomeass.repository.StatisticRunRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.statisticRunRoute(repo: StatisticRunRepo) {

    route("/statistics-runs") {
        get {
            val limit = call.request.queryParameters["limit"]?.let {
                val v = it.toIntOrNull()
                if (v == null || v < 0) return@get call.respond(HttpStatusCode.BadRequest, ErrorDto("Invalid 'limit' value"))
                v
            }

            call.respond(repo.getAll(limit), typeInfo = TypeInfo(ArrayList::class))
        }.describe {
            tag("Statistics")
            summary = "List statistics recorder runs, most recent first"

            parameters {
                query("limit") {
                    required = false
                    schema = jsonSchema<Int>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "Recorder run rows, most recent first, optionally capped by limit. Empty array if no runs recorded."
                    schema = jsonSchema<List<StatisticRunDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "limit present but unparseable or negative."
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
