package com.anjo.monhomeass.routing

import com.anjo.monhomeass.service.EventService
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.eventsRoute(service: EventService) {

    route("/events") {
        get {
           val contain = call.request.queryParameters["contain"]
           val exclude = call.request.queryParameters["exclude"]
            call.respond(service.getAll(contain, exclude),
                    typeInfo = TypeInfo(ArrayList::class))
        }
    }
}