package com.anjo.monhomeass.testutils

import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.Jedis
import java.sql.DriverManager
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

private object TestContainers {
    val postgres: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine")).apply {
            withDatabaseName("home_assistant")
            withUsername("home_ass_monitor")
            withPassword("testpassword")
            start()
        }
    }

    val redis: GenericContainer<Nothing> by lazy {
        GenericContainer<Nothing>(DockerImageName.parse("redis:7-alpine")).apply {
            withExposedPorts(6379)
            start()
        }
    }

    val mariaDb: MariaDBContainer<Nothing> by lazy {
        MariaDBContainer<Nothing>(DockerImageName.parse("mariadb:11")).apply {
            withDatabaseName("home_assistant")
            withUsername("home_ass_monitor")
            withPassword("testpassword")
            start()
        }
    }

    val mySql: MySQLContainer<Nothing> by lazy {
        MySQLContainer<Nothing>(DockerImageName.parse("mysql:8")).apply {
            withDatabaseName("home_assistant")
            withUsername("home_ass_monitor")
            withPassword("testpassword")
            start()
        }
    }
}

fun prepareDatabase() {
    val pg = TestContainers.postgres
    Database.connect(
        url = pg.jdbcUrl,
        driver = "org.postgresql.Driver",
        user = pg.username,
        password = pg.password,
    )
    flushRedis()
}

fun testApplicationConfig(): MapApplicationConfig {
    val pg = TestContainers.postgres
    val redis = TestContainers.redis
    return MapApplicationConfig(
        "ktor.database.user" to pg.username,
        "ktor.database.password" to pg.password,
        "ktor.database.url" to "${pg.host}:${pg.getMappedPort(5432)}",
        "ktor.database.database_name" to pg.databaseName,
        "ktor.database.driver" to "org.postgresql.Driver",
        "ktor.database.database_type" to "postgres",
        "ktor.redis.host" to redis.host,
        "ktor.redis.port" to redis.getMappedPort(6379).toString(),
        "ktor.redis.ttlMillis" to "300000",
    ).apply {
        put("ktor.application.modules", listOf("com.anjo.monhomeass.ApplicationKt.module"))
    }
}

fun prepareDatabaseMariaDb(): Database {
    val db = TestContainers.mariaDb
    val database =
        Database.connect(
            url = db.jdbcUrl,
            driver = "org.mariadb.jdbc.Driver",
            user = db.username,
            password = db.password,
        )
    flushRedis()
    return database
}

fun testApplicationConfigMariaDb(): MapApplicationConfig {
    val db = TestContainers.mariaDb
    val redis = TestContainers.redis
    return MapApplicationConfig(
        "ktor.database.user" to db.username,
        "ktor.database.password" to db.password,
        "ktor.database.url" to "${db.host}:${db.getMappedPort(3306)}",
        "ktor.database.database_name" to db.databaseName,
        "ktor.database.driver" to "org.mariadb.jdbc.Driver",
        "ktor.database.database_type" to "mariadb",
        "ktor.redis.host" to redis.host,
        "ktor.redis.port" to redis.getMappedPort(6379).toString(),
        "ktor.redis.ttlMillis" to "300000",
    ).apply {
        put("ktor.application.modules", listOf("com.anjo.monhomeass.ApplicationKt.module"))
    }
}

fun prepareDatabaseMySql(): Database {
    val db = TestContainers.mySql
    val database =
        Database.connect(
            url = db.jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver",
            user = db.username,
            password = db.password,
        )
    flushRedis()
    return database
}

fun testApplicationConfigMySql(): MapApplicationConfig {
    val db = TestContainers.mySql
    val redis = TestContainers.redis
    return MapApplicationConfig(
        "ktor.database.user" to db.username,
        "ktor.database.password" to db.password,
        "ktor.database.url" to "${db.host}:${db.getMappedPort(3306)}",
        "ktor.database.database_name" to db.databaseName,
        "ktor.database.driver" to "com.mysql.cj.jdbc.Driver",
        "ktor.database.database_type" to "mysql",
        "ktor.redis.host" to redis.host,
        "ktor.redis.port" to redis.getMappedPort(6379).toString(),
        "ktor.redis.ttlMillis" to "300000",
    ).apply {
        put("ktor.application.modules", listOf("com.anjo.monhomeass.ApplicationKt.module"))
    }
}

fun testApplicationConfigSqlite(): MapApplicationConfig {
    val file = createTempFile(prefix = "home-assistant", suffix = ".db")
    file.deleteExisting()
    val redis = TestContainers.redis
    flushRedis()
    return MapApplicationConfig(
        "ktor.database.driver" to "org.sqlite.JDBC",
        "ktor.database.database_type" to "sqlite",
        "ktor.database.file_path" to file.toAbsolutePath().toString(),
        "ktor.redis.host" to redis.host,
        "ktor.redis.port" to redis.getMappedPort(6379).toString(),
        "ktor.redis.ttlMillis" to "300000",
    ).apply {
        put("ktor.application.modules", listOf("com.anjo.monhomeass.ApplicationKt.module"))
    }
}

fun seedSqlite(
    filePath: String,
    seedFunc: () -> Unit,
) {
    val db = Database.connect(url = "jdbc:sqlite:file:$filePath", driver = "org.sqlite.JDBC")
    transaction(db) {
        seedFunc()
    }
    TransactionManager.closeAndUnregister(db)
}

fun flushRedis() {
    val redis = TestContainers.redis
    Jedis(redis.host, redis.getMappedPort(6379)).use { it.flushAll() }
}

fun testRedisHostPort(): Pair<String, Int> {
    val redis = TestContainers.redis
    return redis.host to redis.getMappedPort(6379)
}

fun directUpdate(sql: String) {
    val pg = TestContainers.postgres
    DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { conn ->
        conn.createStatement().use { it.execute(sql) }
    }
}

fun addRecords(insertFunc: () -> InsertStatement<Number>) {
    transaction {
        insertFunc()
    }
}
