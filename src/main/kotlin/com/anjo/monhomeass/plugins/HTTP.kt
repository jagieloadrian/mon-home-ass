package com.anjo.monhomeass.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders


fun Application.configureHTTP() {
    // ponytail: API is readonly, GET-only (see constitution) — CORS only needs to
    // allow the default GET/HEAD/OPTIONS methods, no write verbs or custom headers.
    install(CORS) {
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
}
