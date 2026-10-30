package com.anjo.monhomeass

import com.anjo.monhomeass.plugins.configureDI
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureHTTP
import com.anjo.monhomeass.plugins.configureMonitoring
import com.anjo.monhomeass.plugins.configurePublisher
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureSerialization
import com.anjo.monhomeass.plugins.configureStatusPages
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDI()
    configureSerialization()
    configureDatabases()
    configureMonitoring()
    configureStatusPages()
    configureHTTP()
    configureRouting()
    configurePublisher()
}
