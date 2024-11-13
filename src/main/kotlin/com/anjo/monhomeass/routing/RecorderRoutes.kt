package com.anjo.monhomeass.routing

import com.anjo.monhomeass.repository.RecorderRepo
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.recorderRout(repo: RecorderRepo) {

    route("/recorders") {
        get("/all") {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }
    }
}