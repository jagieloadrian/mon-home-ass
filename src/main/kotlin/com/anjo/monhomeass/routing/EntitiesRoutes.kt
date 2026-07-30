@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.EntityDto
import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.repository.StateMetadataRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.server.response.respond
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.entitiesRoute(repo: StateMetadataRepo) {

    route("/entities") {
        get {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }.describe {
            tag("Entities")
            summary = "List known entity ids"

            responses {
                HttpStatusCode.OK {
                    description = "List of entities."
                    schema = jsonSchema<List<EntityDto>>()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Recorder database unreachable or query failed."
                    schema = jsonSchema<ErrorDto>()
                }
            }
        }
    }
}
