package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.directUpdate
import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StatisticsMetaRoutesKtTest :
    StringSpec({

        fun dropTableIfExists() {
            runCatching { SchemaUtils.drop(Statistic) }
            runCatching { SchemaUtils.drop(StatisticShortTerm) }
            runCatching { SchemaUtils.drop(StatisticMetaData) }
        }

        fun setupWithRecords() {
            prepareDatabase()
            transaction {
                dropTableIfExists()
                SchemaUtils.create(StatisticMetaData)
                addRecords {
                    StatisticMetaData.insert {
                        it[id] = 1
                        it[statisticId] = "sensor.living_room_temperature"
                        it[name] = "Living Room Temperature"
                        it[unitOfMeasurement] = "°C"
                        it[unitClass] = "temperature"
                        it[sourceOfData] = "recorder"
                        it[hasMean] = true
                        it[hasSum] = false
                        it[meanType] = 1
                    }
                }
                addRecords {
                    StatisticMetaData.insert {
                        it[id] = 2
                        it[statisticId] = "sensor.energy_usage"
                        it[name] = null
                        it[unitOfMeasurement] = null
                        it[unitClass] = null
                        it[sourceOfData] = null
                        it[hasMean] = null
                        it[hasSum] = null
                        it[meanType] = 0
                    }
                }
            }
        }

        "given statistics meta when get statistics-meta then returns all entries" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }
                val expected =
                    listOf(
                        StatisticMetaDto(
                            1,
                            "sensor.living_room_temperature",
                            "Living Room Temperature",
                            "°C",
                            "temperature",
                            "recorder",
                            true,
                            false,
                            1,
                        ),
                        StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null, null, 0),
                    )

                // when
                val response =
                    client.get("/statistics-meta") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe expected
            }
        }

        "given search matching statistic_id case-insensitively when get statistics-meta then returns only matching entry" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response =
                    client.get("/statistics-meta?search=ENERGY") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe listOf(StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null, null, 0))
            }
        }

        "given search matching name case-insensitively when get statistics-meta then returns only matching entry" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response =
                    client.get("/statistics-meta?search=room") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe
                    listOf(
                        StatisticMetaDto(
                            1,
                            "sensor.living_room_temperature",
                            "Living Room Temperature",
                            "°C",
                            "temperature",
                            "recorder",
                            true,
                            false,
                            1,
                        ),
                    )
            }
        }

        "given search matching nothing when get statistics-meta then returns empty list" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response =
                    client.get("/statistics-meta?search=nope-not-here") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe emptyList()
            }
        }

        "given blank search when get statistics-meta then returns all entries unchanged" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }
                val expected =
                    listOf(
                        StatisticMetaDto(
                            1,
                            "sensor.living_room_temperature",
                            "Living Room Temperature",
                            "°C",
                            "temperature",
                            "recorder",
                            true,
                            false,
                            1,
                        ),
                        StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null, null, 0),
                    )

                // when
                val response =
                    client.get("/statistics-meta?search=") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe expected
            }
        }

        "given repeated call after direct row mutation when get statistics-meta then returns cached value" {
            testApplication {
                // given
                setupWithRecords()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val first = client.get("/statistics-meta")
                directUpdate("UPDATE statistics_meta SET name = 'changed' WHERE id = 1")
                val second = client.get("/statistics-meta")

                // then
                first.status shouldBe HttpStatusCode.OK
                second.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<List<StatisticMetaDto>>(second.bodyAsText()) shouldBe
                    Json.decodeFromString<List<StatisticMetaDto>>(first.bodyAsText())
            }
        }

        "given no statistics meta when get statistics-meta then returns empty list" {
            testApplication {
                // given
                prepareDatabase()
                transaction {
                    dropTableIfExists()
                    SchemaUtils.create(StatisticMetaData)
                }
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response =
                    client.get("/statistics-meta") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe emptyList()
            }
        }

        "given recorder database query fails when get statistics-meta then returns 503" {
            testApplication {
                // given - table intentionally not (re)created, so the query fails
                prepareDatabase()
                transaction {
                    dropTableIfExists()
                }
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response =
                    client.get("/statistics-meta") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }

                // then
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
