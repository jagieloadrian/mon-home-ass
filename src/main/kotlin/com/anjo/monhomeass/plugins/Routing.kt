package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.routing.eventsRoute
import com.anjo.monhomeass.routing.migrationRoute
import com.anjo.monhomeass.routing.recorderRout
import com.anjo.monhomeass.routing.schemaChangesRout
import com.anjo.monhomeass.service.EventService
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot


fun Application.configureRouting() {
    routing {
        swaggerUI("/swagger") {
            info = OpenApiInfo("Monitor Home Assistant Stats", "1.0")
            source = OpenApiDocSource.Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }
        }
        migrationRoute(MigrationRepo())
        recorderRout(RecorderRepo())
        schemaChangesRout(SchemaChangesRepo())
        eventsRoute(EventService())
    }
}
