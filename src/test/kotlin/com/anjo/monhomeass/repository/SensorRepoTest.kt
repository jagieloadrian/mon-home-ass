package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.homeassistant.HaRegistryClient
import com.anjo.monhomeass.model.dto.HaRegistries
import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.testutils.SEEDED_SENSORS
import com.anjo.monhomeass.testutils.flushRedis
import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.prepareDatabaseMariaDb
import com.anjo.monhomeass.testutils.prepareDatabaseMySql
import com.anjo.monhomeass.testutils.testRedisHostPort
import com.anjo.monhomeass.testutils.withSeededSensors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.jetbrains.exposed.v1.jdbc.Database
import redis.clients.jedis.RedisClient
import kotlin.io.path.createTempFile

class SensorRepoTest :
    StringSpec({

        fun newRepo(registryClient: HaRegistryClient? = null): SensorRepo {
            flushRedis()
            val (host, port) = testRedisHostPort()
            return SensorRepo(RedisCache(RedisClient.create(host, port), 60_000), registryClient)
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

        "should merge the Home Assistant registries into the catalog and fetch them once per cache period when Home Assistant answers" {
            // given
            val registryClient = mockk<HaRegistryClient>()
            coEvery { registryClient.fetch() } returns
                HaRegistries(
                    entities = listOf(HaRegistries.Entity("sensor.kitchen_co2", deviceId = "aqara")),
                    devices = listOf(HaRegistries.Device(id = "aqara", areaId = "kitchen")),
                    areas = listOf(HaRegistries.Area(areaId = "kitchen", name = "Kitchen", floorId = "ground")),
                    floors = listOf(HaRegistries.Floor(floorId = "ground", name = "Ground floor")),
                )
            val repo = newRepo(registryClient)

            // when
            val result =
                withSeededSensors(prepareDatabase()) {
                    repo.getAll()
                    repo.getAll(area = "Kitchen")
                }

            // then
            result shouldBe
                listOf(SensorDto("sensor.kitchen_co2", "Kitchen carbon dioxide", "Kitchen", "Ground floor", "air-quality", "ppm"))
            coVerify(exactly = 1) { registryClient.fetch() }
        }

        "should serve the recorder-only catalog when Home Assistant is unreachable" {
            // given
            val registryClient = mockk<HaRegistryClient>()
            coEvery { registryClient.fetch() } throws IllegalStateException("Home Assistant did not answer within 10s")
            val repo = newRepo(registryClient)

            // when
            val result = withSeededSensors(prepareDatabase()) { repo.getAll() }

            // then
            result shouldBe SEEDED_SENSORS
        }
    })
