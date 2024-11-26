package com.anjo.monhomeass.plugins

import dev.hayden.KHealth
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureMonitoring() {
    install(KHealth)
}
