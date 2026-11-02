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

    val database =
        Database.connect(
            url = url,
            driver = driver,
            user = dbUser,
            password = dbPassword,
            setupConnection = {
                if (type != "sqlite") it.isReadOnly = true
            },
        )
    TransactionManager.defaultDatabase = database
}

fun generateUrl(
    type: String,
    dbUrl: String,
    dbName: String,
    filePath: String = "",
): String =
    when (type) {
        "postgres" -> "jdbc:postgresql://$dbUrl/$dbName"
        "mariadb" -> "jdbc:mariadb://$dbUrl/$dbName"
        "mysql" -> "jdbc:mysql://$dbUrl/$dbName"
        "sqlite" -> "jdbc:sqlite:file:$filePath?mode=ro"
        else -> error("Unsupported database_type: $type. Valid options: postgres, mariadb, mysql, sqlite")
    }
