package com.anjo.monhomeass.plugins

import dev.hayden.KHealth
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

fun Application.configureMonitoring() {
    install(KHealth) {
        healthCheckPath = "live"
        readyCheckPath = "ready"
        unsuccessfulCheckStatusCode = HttpStatusCode.ServiceUnavailable
        readyChecks {
            check("database") { databaseReachable() }
        }
        wrap { healthRoutes ->
            route("/health") {
                hide()
                healthRoutes(this)
            }
        }
    }
}

private fun databaseReachable(): Boolean =
    runCatching {
        transaction {
            maxAttempts = 1
            (connection.connection as Connection).isValid(2)
        }
    }.getOrDefault(false)
