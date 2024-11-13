package com.anjo.monhomeass.routing

import com.anjo.monhomeass.repository.MigrationRepo
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.migrationRout(repo: MigrationRepo) {

    route("/migrations") {
        get("/all") {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }
    }
}