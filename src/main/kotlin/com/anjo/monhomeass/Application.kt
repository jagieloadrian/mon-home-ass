package com.anjo.monhomeass

import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureHTTP
import com.anjo.monhomeass.plugins.configureMonitoring
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
