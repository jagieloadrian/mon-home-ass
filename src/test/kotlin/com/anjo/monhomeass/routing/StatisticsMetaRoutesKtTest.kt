package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTem
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test

class StatisticsMetaRoutesKtTest {

    @Test
    fun `given statistics meta when get statistics-meta then returns all entries`() = testApplication {
        //given
        setupWithRecords()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }
        val expected = listOf(
                StatisticMetaDto(1, "sensor.living_room_temperature", "Living Room Temperature", "°C", "recorder", true, false),
                StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null)
        )

        //when
        val response = client.get("/statistics-meta") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    @Test
    fun `given search matching statistic_id case-insensitively when get statistics-meta then returns only matching entry`() = testApplication {
        //given
        setupWithRecords()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }

        //when
        val response = client.get("/statistics-meta?search=ENERGY") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null))
    }

    @Test
    fun `given search matching name case-insensitively when get statistics-meta then returns only matching entry`() = testApplication {
        //given
        setupWithRecords()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }

        //when
        val response = client.get("/statistics-meta?search=room") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe listOf(
                StatisticMetaDto(1, "sensor.living_room_temperature", "Living Room Temperature", "°C", "recorder", true, false)
        )
    }

    @Test
    fun `given search matching nothing when get statistics-meta then returns empty list`() = testApplication {
        //given
        setupWithRecords()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }

        //when
        val response = client.get("/statistics-meta?search=nope-not-here") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe emptyList()
    }

    @Test
    fun `given blank search when get statistics-meta then returns all entries unchanged`() = testApplication {
        //given
        setupWithRecords()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }
        val expected = listOf(
                StatisticMetaDto(1, "sensor.living_room_temperature", "Living Room Temperature", "°C", "recorder", true, false),
                StatisticMetaDto(2, "sensor.energy_usage", null, null, null, null, null)
        )

        //when
        val response = client.get("/statistics-meta?search=") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    @Test
    fun `given no statistics meta when get statistics-meta then returns empty list`() = testApplication {
        //given
        prepareDatabase()
        transaction {
            dropTableIfExists()
            SchemaUtils.create(StatisticMetaData)
        }
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }

        //when
        val response = client.get("/statistics-meta") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val decodedJson = Json.decodeFromString<List<StatisticMetaDto>>(response.bodyAsText())

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe emptyList()
    }

    @Test
    fun `given recorder database query fails when get statistics-meta then returns 503`() = testApplication {
        //given - table intentionally not (re)created, so the query fails
        prepareDatabase()
        transaction {
            dropTableIfExists()
        }
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureStatusPages()
            configureRouting()
        }

        //when
        val response = client.get("/statistics-meta") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }

        //then
        response.status shouldBe HttpStatusCode.ServiceUnavailable
    }

    private fun setupWithRecords() {
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
                    it[sourceOfData] = "recorder"
                    it[hasMean] = true
                    it[hasSum] = false
                }
            }
            addRecords {
                StatisticMetaData.insert {
                    it[id] = 2
                    it[statisticId] = "sensor.energy_usage"
                    it[name] = null
                    it[unitOfMeasurement] = null
                    it[sourceOfData] = null
                    it[hasMean] = null
                    it[hasSum] = null
                }
            }
        }
    }

    // ponytail: TestDatabase's H2 instance is a shared named in-memory DB
    // (DB_CLOSE_DELAY=-1) reused across every test class in the suite — drop
    // Statistic/StatisticShortTem first since they FK into StatisticMetaData
    // (other test classes may have left them behind), then drop this table,
    // so each test starts from a clean/absent table regardless of run order.
    private fun dropTableIfExists() {
        runCatching { SchemaUtils.drop(Statistic) }
        runCatching { SchemaUtils.drop(StatisticShortTem) }
        runCatching { SchemaUtils.drop(StatisticMetaData) }
    }
}
