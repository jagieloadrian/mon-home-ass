@file:OptIn(ExperimentalKtorApi::class)

package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.service.EventService
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi

fun Route.eventsRoute(service: EventService) {

    route("/events") {
        get {
           val contain = call.request.queryParameters["contain"]
           val exclude = call.request.queryParameters["exclude"]
            call.respond(status = HttpStatusCode.OK, message = service.getAll(contain, exclude))
        }.describe {
            tag("Event")
            summary = "Get info about events"

            responses {
                HttpStatusCode.OK {
                    description = "received data"
                    schema = jsonSchema<List<EventDto>>()
                }
            }
        }
    }
}