package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.SchemaChangeDto
import com.anjo.monhomeass.repository.SchemaChangesRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route

fun Route.schemaChangesRout(repo: SchemaChangesRepo) {

    route("/schemas") {
        get {
            call.respond(status = HttpStatusCode.OK, message = repo.getAll())
        }.describe {
            tag("Schema Changes")
            summary = "Get info about schema changes"

            responses {
                HttpStatusCode.OK {
                    description = "received data"
                    schema = jsonSchema<List<SchemaChangeDto>>()
                }
            }
        }
    }
}