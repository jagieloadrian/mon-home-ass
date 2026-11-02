package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.StatisticSummaryDto
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class StatisticSummaryRoutesKtTest :
    StringSpec({

        fun dropTablesIfExists() {
            runCatching { SchemaUtils.drop(Statistic) }
            runCatching { SchemaUtils.drop(StatisticShortTerm) }
            runCatching { SchemaUtils.drop(StatisticMetaData) }
        }

        fun setupWithRecords() {
            prepareDatabase()
            transaction {
                dropTablesIfExists()
                SchemaUtils.create(StatisticMetaData, Statistic)
                addRecords {
                    StatisticMetaData.insert {
                        it[id] = 1
                        it[statisticId] = "sensor.living_room_temperature"
                        it[name] = "Living Room Temperature"
                        it[unitOfMeasurement] = "°C"
                        it[sourceOfData] = "recorder"
                        it[hasMean] = true
                        it[hasSum] = false
                        it[meanType] = 0
                    }
                }
                addRecords {
                    Statistic.insert {
                        it[id] = 1
                        it[metadataId] = 1
                        it[startTs] = Instant.fromEpochSeconds(1_700_000_000L)
                        it[state] = 9.0
                    }
                }
                addRecords {
                    Statistic.insert {
                        it[id] = 2
                        it[metadataId] = 1
                        it[startTs] = Instant.fromEpochSeconds(1_700_003_600L)
                        it[state] = 11.0
                    }
                }
            }
        }

        "given known statisticId when get summary then returns aggregate over full history" {
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary")
                val decodedJson = Json.decodeFromString<StatisticSummaryDto>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe StatisticSummaryDto("sensor.living_room_temperature", null, null, 9.0, 11.0, 10.0, 20.0)
            }
        }

        "given from and to query params when get summary then aggregates only in-range rows" {
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary?from=1700003000&to=1700004000")
                val decodedJson = Json.decodeFromString<StatisticSummaryDto>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe
                    StatisticSummaryDto(
                        "sensor.living_room_temperature",
                        Instant.fromEpochSeconds(1700003000L).toString(),
                        Instant.fromEpochSeconds(1700004000L).toString(),
                        11.0,
                        11.0,
                        11.0,
                        11.0,
                    )
            }
        }

        "given known statisticId with no rows in range when get summary then returns 200 with null aggregates" {
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary?from=1600000000&to=1600000001")
                val decodedJson = Json.decodeFromString<StatisticSummaryDto>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe
                    StatisticSummaryDto(
                        "sensor.living_room_temperature",
                        Instant.fromEpochSeconds(1600000000L).toString(),
                        Instant.fromEpochSeconds(1600000001L).toString(),
                        null,
                        null,
                        null,
                        null,
                    )
            }
        }

        "given repeated call after direct row mutation when get summary then returns cached value" {
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
                val first = client.get("/statistics/sensor.living_room_temperature/summary")
                directUpdate("UPDATE statistics SET state = 999 WHERE id = 1")
                val second = client.get("/statistics/sensor.living_room_temperature/summary")

                // then
                first.status shouldBe HttpStatusCode.OK
                second.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<StatisticSummaryDto>(second.bodyAsText()) shouldBe
                    Json.decodeFromString<StatisticSummaryDto>(first.bodyAsText())
            }
        }

        "given unknown statisticId when get summary then returns 404" {
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
                val response = client.get("/statistics/not-a-real-id/summary")

                // then
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        "given unparseable from when get summary then returns 400" {
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary?from=not-a-date")

                // then
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "given unparseable to when get summary then returns 400" {
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary?to=not-a-date")

                // then
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        "given recorder database query fails when get summary then returns 503" {
            testApplication {
                // given - table intentionally not (re)created, so the query fails
                prepareDatabase()
                transaction {
                    dropTablesIfExists()
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
                val response = client.get("/statistics/sensor.living_room_temperature/summary")

                // then
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
