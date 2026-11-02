package com.anjo.monhomeass.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
}
