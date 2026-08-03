package com.anjo.monhomeass.plugins

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabases() {
    val dbConfig = environment.config.config("ktor.database")
    val dbUrl = dbConfig.propertyOrNull("url")?.getString() ?: "localhost:5432"
    val dbName = dbConfig.propertyOrNull("database_name")?.getString() ?: "postgres"
    val dbUser = dbConfig.propertyOrNull("user")?.getString() ?: "postgres"
    val dbPassword = dbConfig.propertyOrNull("password")?.getString() ?: ""
    val driver = dbConfig.propertyOrNull("driver")?.getString() ?: ""
    val type = dbConfig.propertyOrNull("database_type")?.getString() ?: ""
    val url = generateUrl(type, dbUrl, dbName)

    Database.connect(
            url = url,
            driver = driver,
            user = dbUser,
            password = dbPassword,
            setupConnection = {
                it.isReadOnly = true
            }
    )
}

fun generateUrl(type: String, dbUrl: String, dbName: String): String {
    require(type == "postgres") { "Unsupported database_type: $type" }
    return "jdbc:postgresql://$dbUrl/$dbName"
}
