package com.anjo.monhomeass.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders

fun Application.configureHTTP() {
    val corsHosts =
        environment.config
            .propertyOrNull("ktor.cors.hosts")
            ?.getString()
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
    install(CORS) {
        corsHosts.forEach { allowHost(it, schemes = listOf("http", "https")) }
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }
}
