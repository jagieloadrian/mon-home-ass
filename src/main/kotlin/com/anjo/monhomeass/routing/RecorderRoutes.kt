package com.anjo.monhomeass.routing

import com.anjo.monhomeass.repository.RecorderRepo
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.util.reflect.TypeInfo

fun Route.recorderRout(repo: RecorderRepo) {

    route("/recorders") {
        get {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }
    }
}