package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.repository.StateMetadataRepo
import com.anjo.monhomeass.repository.StatisticMetaRepo
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.routing.entitiesRoute
import com.anjo.monhomeass.routing.eventsRoute
import com.anjo.monhomeass.routing.migrationRoute
import com.anjo.monhomeass.routing.recorderRout
import com.anjo.monhomeass.routing.schemaChangesRout
import com.anjo.monhomeass.routing.statisticMetaRoute
import com.anjo.monhomeass.routing.statisticRoute
import com.anjo.monhomeass.routing.statisticShortTermRoute
import com.anjo.monhomeass.routing.statisticSummaryRoute
import com.anjo.monhomeass.service.EventService
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot

fun Application.configureRouting() {
    val eventService:EventService by dependencies
    val migrationRepo: MigrationRepo by dependencies
    val recorderRepo: RecorderRepo by dependencies
    val schemaChangesRepo: SchemaChangesRepo by dependencies
    val stateMetadataRepo: StateMetadataRepo by dependencies
    val statisticMetaRepo: StatisticMetaRepo by dependencies
    val statisticRepo: StatisticRepo by dependencies
    routing {
        migrationRoute(migrationRepo)
        recorderRout(recorderRepo)
        schemaChangesRout(schemaChangesRepo)
        eventsRoute(eventService)
        entitiesRoute(stateMetadataRepo)
        statisticMetaRoute(statisticMetaRepo)
        statisticRoute(statisticRepo)
        statisticShortTermRoute(statisticRepo)
        statisticSummaryRoute(statisticRepo)
    }

    // ponytail: OpenApiDocSource.Routing's built-in lazy-async generation (the
    // swaggerUI default) silently returns an empty document when read through
    // Ktor's own async(LAZY) + coroutine-dispatch path in this Ktor version —
    // reproduced and isolated to that mechanism specifically (a direct,
    // synchronous .read() call works correctly). Building the doc eagerly
    // here, once, synchronously, right after every route is registered (and
    // the RoutingRoot plugin is installed) sidesteps the bug entirely and
    // also means every route's `.describe {}` block actually runs (and is
    // covered) at startup instead of never running at all.
    val openApiDoc = OpenApiDocSource.Routing(ContentType.Application.Json) {
        routingRoot.descendants()
    }.read(this, OpenApiDoc(info = OpenApiInfo("Monitor Home Assistant Stats", "1.0")))

    routing {
        swaggerUI("/swagger") {
            source = OpenApiDocSource.Text(openApiDoc.content, openApiDoc.contentType)
        }
    }
}
