package com.anjo.monhomeass.plugins

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

fun Application.configureDatabases() {
    val dbConfig = environment.config.config("ktor.database")
    val dbUrl = dbConfig.propertyOrNull("url")?.getString() ?: "localhost:5432"
    val dbName = dbConfig.propertyOrNull("database_name")?.getString() ?: "postgres"
    val dbUser = dbConfig.propertyOrNull("user")?.getString() ?: "postgres"
    val dbPassword = dbConfig.propertyOrNull("password")?.getString() ?: ""
    val driver = dbConfig.propertyOrNull("driver")?.getString() ?: ""
    val type = dbConfig.propertyOrNull("database_type")?.getString() ?: ""
    val filePath = dbConfig.propertyOrNull("file_path")?.getString() ?: ""
    val url = generateUrl(type, dbUrl, dbName, filePath)

    val database = Database.connect(
            url = url,
            driver = driver,
            user = dbUser,
            password = dbPassword,
            setupConnection = {
                // sqlite-jdbc throws on setReadOnly() after connect — its URL's mode=ro already
                // enforces read-only at the driver level. Network engines honor setReadOnly().
                if (type != "sqlite") it.isReadOnly = true
            }
    )
    // Exposed falls back to "last database created" when no default is set explicitly —
    // racy once more than one engine is ever connected in the same JVM (e.g. across tests).
    // Pin this app's own connection as the default so ambient transaction {} blocks never
    // silently target a stale connection.
    TransactionManager.defaultDatabase = database
}

fun generateUrl(type: String, dbUrl: String, dbName: String, filePath: String = ""): String {
    return when (type) {
        "postgres" -> "jdbc:postgresql://$dbUrl/$dbName"
        "mariadb" -> "jdbc:mariadb://$dbUrl/$dbName"
        "mysql" -> "jdbc:mysql://$dbUrl/$dbName"
        "sqlite" -> "jdbc:sqlite:file:$filePath?mode=ro"
        else -> error("Unsupported database_type: $type. Valid options: postgres, mariadb, mysql, sqlite")
    }
}
