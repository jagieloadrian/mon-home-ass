@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.repository.SensorRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.sensorsRoute(repo: SensorRepo) {
    route("/sensors") {
        get {
            call.respond(
                repo.getAll(call.request.queryParameters["area"], call.request.queryParameters["category"]),
                typeInfo = TypeInfo(ArrayList::class),
            )
        }.describe {
            tag("Sensors")
            summary = "List sensors with a human name, area, floor, category and unit"

            parameters {
                query("area") {
                    required = false
                    description = "Exact area name."
                    schema = jsonSchema<String>()
                }
                query("category") {
                    required = false
                    description =
                        "Exact category: climate, air-quality, energy, utility, battery, light, diagnostic or other."
                    schema = jsonSchema<String>()
                }
            }

            responses {
                HttpStatusCode.OK {
                    description = "One sensor per statistic, ordered by entity id, optionally filtered."
                    schema = jsonSchema<List<SensorDto>>()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Recorder database unreachable or query failed."
                    schema = jsonSchema<ErrorDto>()
                }
            }
        }
    }
}
