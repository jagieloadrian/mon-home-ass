package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.MigrationDto
import com.anjo.monhomeass.repository.MigrationRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Route.migrationRoute(repo: MigrationRepo) {

    route("/migrations") {
        get {
            call.respond(status = HttpStatusCode.OK, message = repo.getAll())
        }.describe {
            tag("Migration")
            summary = "Get info about migrations"

            responses {
                HttpStatusCode.OK {
                    description = "received data"
                    schema = jsonSchema<List<MigrationDto>>()
                }
            }
        }
    }
}