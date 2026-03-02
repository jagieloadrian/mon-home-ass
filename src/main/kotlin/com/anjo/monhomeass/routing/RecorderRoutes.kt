package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.model.dto.RecorderDto
import com.anjo.monhomeass.repository.RecorderRepo
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.util.reflect.TypeInfo

fun Route.recorderRout(repo: RecorderRepo) {

    route("/recorders") {
        get {
            call.respond(repo.getAll(),
                    typeInfo = TypeInfo(ArrayList::class))
        }.describe {
            tag("Recorder")
            summary = "Get info about recorders"

            responses {
                HttpStatusCode.OK {
                    description = "received data"
                    schema = jsonSchema<List<RecorderDto>>()
                }
            }
        }
    }
}