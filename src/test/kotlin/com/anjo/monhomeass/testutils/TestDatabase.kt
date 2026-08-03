package com.anjo.monhomeass.testutils

import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import redis.clients.jedis.Jedis
import java.sql.DriverManager

// ponytail: one Postgres + one Redis container shared for the whole test
// JVM (lazy singleton, never stopped) — mirrors the previous shared-named-H2
// pattern, just with real engines. Started on first use, torn down when the
// JVM exits.
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
}

// ponytail: also flushes Redis on every call — prepareDatabase() already
// runs at the start of every test in every file, so piggybacking the
// flush here means every test starts with a clean cache too, without
// touching every individual test file.
fun prepareDatabase() {
    val pg = TestContainers.postgres
    Database.connect(
            url = pg.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = pg.username,
            password = pg.password
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
            "ktor.redis.ttlMillis" to "300000"
    ).apply {
        // ponytail: previous H2-era config file relied on ktor.application.modules
        // to auto-invoke the real module() (serialization/DI/etc.) alongside each
        // test's own explicit application {} block — replicate that here since
        // MapApplicationConfig's vararg constructor can't express a list value.
        put("ktor.application.modules", listOf("com.anjo.monhomeass.ApplicationKt.module"))
    }
}

fun flushRedis() {
    val redis = TestContainers.redis
    Jedis(redis.host, redis.getMappedPort(6379)).use { it.flushAll() }
}

fun testRedisHostPort(): Pair<String, Int> {
    val redis = TestContainers.redis
    return redis.host to redis.getMappedPort(6379)
}

// ponytail: bypasses Exposed's globally registered connection, which the
// app itself sets read-only via configureDatabases() — a normal
// transaction {} block would fail here on real Postgres (H2 didn't enforce
// this). Used only to mutate a row mid-test to prove cache staleness.
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
