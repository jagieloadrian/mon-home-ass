package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.routing.migrationRout
import com.anjo.monhomeass.routing.recorderRout
import com.anjo.monhomeass.routing.schemaChangesRout
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        migrationRout(MigrationRepo())
        recorderRout(RecorderRepo())
        schemaChangesRout(SchemaChangesRepo())
    }
}
