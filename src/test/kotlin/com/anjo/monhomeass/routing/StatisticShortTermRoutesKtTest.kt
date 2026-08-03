package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.StatisticDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTem
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.directUpdate
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class StatisticShortTermRoutesKtTest : StringSpec({

    // ponytail: shared named in-memory H2 DB reused across every test class in
    // the suite — drop Statistic/StatisticShortTem first since they FK into
    // StatisticMetaData (other test classes may have left them behind), so
    // each test starts clean regardless of run order.
    fun dropTablesIfExists() {
        runCatching { SchemaUtils.drop(Statistic) }
        runCatching { SchemaUtils.drop(StatisticShortTem) }
        runCatching { SchemaUtils.drop(StatisticMetaData) }
    }

    fun setupWithRecords() {
        prepareDatabase()
        transaction {
            dropTablesIfExists()
            SchemaUtils.create(StatisticMetaData, StatisticShortTem)
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
                StatisticShortTem.insert {
                    it[id] = 1
                    it[metadataId] = 1
                    it[startTs] = Instant.fromEpochSeconds(1_700_000_000L)
                    it[mean] = 10.0
                    it[min] = 5.0
                    it[max] = 15.0
                }
            }
            addRecords {
                StatisticShortTem.insert {
                    it[id] = 2
                    it[metadataId] = 1
                    it[startTs] = Instant.fromEpochSeconds(1_700_000_300L)
                    it[mean] = 12.0
                    it[min] = 6.0
                    it[max] = 18.0
                }
            }
        }
    }

    "given known statisticId when get statistics id short-term then returns all rows oldest first" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }
            val expected = listOf(
                    StatisticDto(1_700_000_000L, 10.0, null, 5.0, 15.0, null, null),
                    StatisticDto(1_700_000_300L, 12.0, null, 6.0, 18.0, null, null)
            )

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
            }
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe expected
        }
    }

    "given limit when get statistics id short-term then returns only first N rows" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?limit=1")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe listOf(StatisticDto(1_700_000_000L, 10.0, null, 5.0, 15.0, null, null))
        }
    }

    "given limit and offset when get statistics id short-term then returns next page" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?limit=1&offset=1")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe listOf(StatisticDto(1_700_000_300L, 12.0, null, 6.0, 18.0, null, null))
        }
    }

    "given offset beyond total rows when get statistics id short-term then returns empty list" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?offset=9999")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe emptyList()
        }
    }

    "given negative limit when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?limit=-1")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given non-numeric limit when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?limit=abc")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given negative offset when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?offset=-1")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given non-numeric offset when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?offset=abc")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given unparseable to when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?to=not-a-date")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given repeated call after direct row mutation when get statistics id short-term then returns cached value" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val first = client.get("/statistics/sensor.living_room_temperature/short-term")
            directUpdate("UPDATE statistics_short_term SET mean = 999 WHERE id = 1")
            val second = client.get("/statistics/sensor.living_room_temperature/short-term")

            //then
            first.status shouldBe HttpStatusCode.OK
            second.status shouldBe HttpStatusCode.OK
            Json.decodeFromString<List<StatisticDto>>(second.bodyAsText()) shouldBe
                    Json.decodeFromString<List<StatisticDto>>(first.bodyAsText())
        }
    }

    "given from and to query params when get statistics id short-term then returns only in-range rows" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when: epoch seconds, both bounds
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?from=1700000200&to=1700000400")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe listOf(StatisticDto(1_700_000_300L, 12.0, null, 6.0, 18.0, null, null))
        }
    }

    "given ISO-8601 from and to when get statistics id short-term then returns only in-range rows" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?from=2023-11-14T22:16:40Z&to=2023-11-14T22:20:00Z")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe listOf(StatisticDto(1_700_000_300L, 12.0, null, 6.0, 18.0, null, null))
        }
    }

    "given from after to when get statistics id short-term then returns empty list" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?from=1700000400&to=1700000000")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe emptyList()
        }
    }

    "given unparseable from when get statistics id short-term then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?from=not-a-date")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given unknown statisticId when get statistics id short-term then returns 404" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/not-a-real-id/short-term")

            //then
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    "given known statisticId with no rows in range when get statistics id short-term then returns 200 empty list" {
        testApplication {
            //given
            setupWithRecords()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/statistics/sensor.living_room_temperature/short-term?from=1600000000&to=1600000001")
            val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decodedJson shouldBe emptyList()
        }
    }
})
