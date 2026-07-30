package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.utils.Constants.DATABASE_CONFIG_PREFIX
import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabases() {
    val dbConfig = environment.config.config(DATABASE_CONFIG_PREFIX)
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
    return when(type) {
        "postgres" -> "jdbc:postgresql://$dbUrl/$dbName"
        "h2" -> "jdbc:h2:$dbUrl:$dbName"
        else -> throw IllegalArgumentException("")
    }
}
