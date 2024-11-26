package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.routing.eventsRoute
import com.anjo.monhomeass.routing.migrationRoute
import com.anjo.monhomeass.routing.recorderRout
import com.anjo.monhomeass.routing.schemaChangesRout
import com.anjo.monhomeass.service.EventService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing


fun Application.configureRouting() {
    routing {
        migrationRoute(MigrationRepo())
        recorderRout(RecorderRepo())
        schemaChangesRout(SchemaChangesRepo())
        eventsRoute(EventService())
    }
}
