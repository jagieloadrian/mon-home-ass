package com.anjo.monhomeass.routing

import com.anjo.monhomeass.repository.SchemaChangesRepo
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.schemaChangesRout(repo: SchemaChangesRepo) {

    route("/schemas") {
        get("/all") {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }
    }
}