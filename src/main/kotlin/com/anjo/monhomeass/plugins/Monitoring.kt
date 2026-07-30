package com.anjo.monhomeass.plugins

import dev.hayden.KHealth
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.openapi.hide

fun Application.configureMonitoring() {
    install(KHealth) {
        wrap {
            this.hide()
        }
    }
}
