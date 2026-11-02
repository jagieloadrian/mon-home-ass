package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.EventRepo
import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.repository.StateMetadataRepo
import com.anjo.monhomeass.repository.StateRepo
import com.anjo.monhomeass.repository.StatisticMetaRepo
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.repository.StatisticRunRepo
import com.anjo.monhomeass.routing.entitiesRoute
import com.anjo.monhomeass.routing.eventsRoute
import com.anjo.monhomeass.routing.migrationRoute
import com.anjo.monhomeass.routing.recorderRoute
import com.anjo.monhomeass.routing.schemaChangesRoute
import com.anjo.monhomeass.routing.stateHistoryRoute
import com.anjo.monhomeass.routing.stateRoute
import com.anjo.monhomeass.routing.statisticMetaRoute
import com.anjo.monhomeass.routing.statisticRoute
import com.anjo.monhomeass.routing.statisticRunRoute
import com.anjo.monhomeass.routing.statisticShortTermRoute
import com.anjo.monhomeass.routing.statisticSummaryRoute
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot

fun Application.configureRouting() {
    val eventRepo: EventRepo by dependencies
    val migrationRepo: MigrationRepo by dependencies
    val recorderRepo: RecorderRepo by dependencies
    val schemaChangesRepo: SchemaChangesRepo by dependencies
    val stateMetadataRepo: StateMetadataRepo by dependencies
    val statisticMetaRepo: StatisticMetaRepo by dependencies
    val statisticRepo: StatisticRepo by dependencies
    val stateRepo: StateRepo by dependencies
    val statisticRunRepo: StatisticRunRepo by dependencies
    routing {
        migrationRoute(migrationRepo)
        recorderRoute(recorderRepo)
        schemaChangesRoute(schemaChangesRepo)
        eventsRoute(eventRepo)
        entitiesRoute(stateMetadataRepo)
        statisticMetaRoute(statisticMetaRepo)
        statisticRoute(statisticRepo)
        statisticShortTermRoute(statisticRepo)
        statisticSummaryRoute(statisticRepo)
        stateRoute(stateRepo)
        stateHistoryRoute(stateRepo)
        statisticRunRoute(statisticRunRepo)
    }

    val openApiDocInfo = OpenApiDoc(info = OpenApiInfo("Monitor Home Assistant Stats", "1.0"))
    val openApiDoc =
        OpenApiDocSource
            .Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }.read(this, openApiDocInfo)
    val openApiDocYaml =
        OpenApiDocSource
            .Routing(ContentType.Application.Yaml) {
                routingRoot.descendants()
            }.read(this, openApiDocInfo)

    routing {
        swaggerUI("/swagger") {
            source = OpenApiDocSource.Text(openApiDoc.content, openApiDoc.contentType)
        }
        get("/openapi.json") {
            call.respondText(openApiDoc.content, openApiDoc.contentType)
        }
        get("/openapi.yaml") {
            call.respondText(openApiDocYaml.content, openApiDocYaml.contentType)
        }
    }
}
