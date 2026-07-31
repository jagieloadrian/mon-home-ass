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
import kotlin.test.Test
import kotlin.time.Instant

class StatisticLongTermRoutesKtTest {

    @Test
    fun `given known statisticId when get statistics id then returns all rows oldest first`() = testApplication {
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
                StatisticDto(1_700_000_000L, 10.0, 5.0, 15.0, null, null),
                StatisticDto(1_700_003_600L, 12.0, 6.0, 18.0, null, null)
        )

        //when
        val response = client.get("/statistics/sensor.living_room_temperature") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    @Test
    fun `given limit when get statistics id then returns only first N rows`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?limit=1")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(StatisticDto(1_700_000_000L, 10.0, 5.0, 15.0, null, null))
    }

    @Test
    fun `given limit and offset when get statistics id then returns next page`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?limit=1&offset=1")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(StatisticDto(1_700_003_600L, 12.0, 6.0, 18.0, null, null))
    }

    @Test
    fun `given offset beyond total rows when get statistics id then returns empty list`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?offset=9999")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe emptyList()
    }

    @Test
    fun `given negative limit when get statistics id then returns 400`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?limit=-1")

        //then
        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `given repeated call after direct row mutation when get statistics id then returns cached value`() = testApplication {
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
        val first = client.get("/statistics/sensor.living_room_temperature")
        directUpdate("UPDATE statistics SET mean = 999 WHERE id = 1")
        val second = client.get("/statistics/sensor.living_room_temperature")

        //then
        first.status shouldBe HttpStatusCode.OK
        second.status shouldBe HttpStatusCode.OK
        Json.decodeFromString<List<StatisticDto>>(second.bodyAsText()) shouldBe
                Json.decodeFromString<List<StatisticDto>>(first.bodyAsText())
    }

    @Test
    fun `given from and to query params when get statistics id then returns only in-range rows`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?from=1700003000&to=1700004000")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(StatisticDto(1_700_003_600L, 12.0, 6.0, 18.0, null, null))
    }

    @Test
    fun `given ISO-8601 from and to when get statistics id then returns only in-range rows`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?from=2023-11-14T23:03:20Z&to=2023-11-14T23:20:00Z")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(StatisticDto(1_700_003_600L, 12.0, 6.0, 18.0, null, null))
    }

    @Test
    fun `given from after to when get statistics id then returns empty list`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?from=1700004000&to=1700000000")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe emptyList()
    }

    @Test
    fun `given unparseable from when get statistics id then returns 400`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?from=not-a-date")

        //then
        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `given unknown statisticId when get statistics id then returns 404`() = testApplication {
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
        val response = client.get("/statistics/not-a-real-id")

        //then
        response.status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `given known statisticId with no rows in range when get statistics id then returns 200 empty list`() = testApplication {
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
        val response = client.get("/statistics/sensor.living_room_temperature?from=1600000000&to=1600000001")
        val decodedJson = Json.decodeFromString<List<StatisticDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe emptyList()
    }

    private fun setupWithRecords() {
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
                }
            }
            addRecords {
                Statistic.insert {
                    it[id] = 1
                    it[metadataId] = 1
                    it[startTs] = Instant.fromEpochSeconds(1_700_000_000L)
                    it[mean] = 10.0
                    it[min] = 5.0
                    it[max] = 15.0
                }
            }
            addRecords {
                Statistic.insert {
                    it[id] = 2
                    it[metadataId] = 1
                    it[startTs] = Instant.fromEpochSeconds(1_700_003_600L)
                    it[mean] = 12.0
                    it[min] = 6.0
                    it[max] = 18.0
                }
            }
        }
    }

    // ponytail: shared named in-memory H2 DB reused across every test class in
    // the suite — drop Statistic/StatisticShortTem first since they FK into
    // StatisticMetaData, so each test starts clean regardless of run order.
    private fun dropTablesIfExists() {
        runCatching { SchemaUtils.drop(Statistic) }
        runCatching { SchemaUtils.drop(StatisticShortTem) }
        runCatching { SchemaUtils.drop(StatisticMetaData) }
    }
}
