package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.testutils.SEEDED_SENSORS
import com.anjo.monhomeass.testutils.flushRedis
import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.prepareDatabaseMariaDb
import com.anjo.monhomeass.testutils.prepareDatabaseMySql
import com.anjo.monhomeass.testutils.testRedisHostPort
import com.anjo.monhomeass.testutils.withSeededSensors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import redis.clients.jedis.RedisClient
import kotlin.io.path.createTempFile

class SensorRepoTest :
    StringSpec({

        fun newRepo(): SensorRepo {
            flushRedis()
            val (host, port) = testRedisHostPort()
            return SensorRepo(RedisCache(RedisClient.create(host, port), 60_000))
        }

        val engines =
            listOf(
                "postgres" to ::prepareDatabase,
                "mariadb" to ::prepareDatabaseMariaDb,
                "mysql" to ::prepareDatabaseMySql,
                "sqlite" to { Database.connect("jdbc:sqlite:file:${createTempFile(suffix = ".db")}", "org.sqlite.JDBC") },
            )

        engines.forEach { (engine, connect) ->
            "should build the catalog from the latest state attributes and statistics meta when the recorder runs on $engine" {
                // given
                val repo = newRepo()

                // when
                val result = withSeededSensors(connect()) { repo.getAll() }

                // then
                result shouldBe SEEDED_SENSORS
            }
        }

        "should return only the sensors of a category when filtered by category" {
            // given
            val repo = newRepo()

            // when
            val result = withSeededSensors(prepareDatabase()) { repo.getAll(category = "climate") }

            // then
            result shouldBe SEEDED_SENSORS.filter { it.category == "climate" }
        }
    })
