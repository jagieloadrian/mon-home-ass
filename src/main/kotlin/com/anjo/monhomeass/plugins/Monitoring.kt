package com.anjo.monhomeass.plugins

import dev.hayden.KHealth
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.route

fun Application.configureMonitoring() {
    install(KHealth) {
        healthCheckPath = "live"
        readyCheckPath = "ready"
        wrap { healthRoutes ->
            route("/health") {
                hide()
                healthRoutes(this)
            }
        }
    }
}
