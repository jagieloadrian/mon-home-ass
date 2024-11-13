package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.utils.Constants.DATABASE_CONFIG_PREFIX
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val dbConfig = environment.config.config(DATABASE_CONFIG_PREFIX)
    val dbUrl = dbConfig.propertyOrNull("url")?.getString() ?: "localhost:5432"
    val dbName = dbConfig.propertyOrNull("database_name")?.getString() ?: "postgres"
    val dbUser = dbConfig.propertyOrNull("user")?.getString() ?: "postgres"
    val dbPassword = dbConfig.propertyOrNull("password")?.getString() ?: ""
    val url = "jdbc:postgresql://$dbUrl/$dbName"

    Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword,
            setupConnection = {
                it.isReadOnly = true

            }
    )
}
