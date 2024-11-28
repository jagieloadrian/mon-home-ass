package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.DataContentDto
import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.model.dto.EventTypeDto
import com.anjo.monhomeass.model.tables.Event
import com.anjo.monhomeass.model.tables.EventData
import com.anjo.monhomeass.model.tables.EventType
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.datetime.Instant as kInstant
import java.time.Instant as jInstant

class EventsRoutesKtTest {

    @BeforeTest
    fun setup() {
        setupWithRecord()
    }

    @AfterTest
    fun teardown() {
        transaction {
            Event.deleteAll()
            EventType.deleteAll()
            EventData.deleteAll()
        }
    }
    //TODO() fix tests

    @Test
    fun `given event routes when get the return events`() = testApplication {
        //given
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val dateTime = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val eventDto = EventDto(1, "T", "D", "O", 1, dateTime, Instant.fromEpochSeconds(1),
                "C", "U", "P", DataContentDto(2, 2L, "This is sharedData, exclude"),
                EventTypeDto(3, "Type of eventType"))
        val expected = listOf(eventDto)

        //when
        val response = client.get("/events") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val responseBody = response.bodyAsText()
        val decodedJson = Json.decodeFromString<List<EventDto>>(responseBody)

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    @Test
    fun `given event routes with exclude when get the return events`() = testApplication {
        //given
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val dateTime = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val eventDto = EventDto(1, "T", "D", "O", 1, dateTime, Instant.fromEpochSeconds(1),
                "C", "U", "P", DataContentDto(2, 2L, "This is sharedData, exclude"),
                EventTypeDto(3, "Type of eventType"))
        val expected = listOf(eventDto)

        //when
        val response = client.get("/events") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            parameter("exclude", "exclude")
        }
        val responseBody = response.bodyAsText()
        val decodedJson = Json.decodeFromString<List<EventDto>>(responseBody)

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    private fun setupWithRecord() {
        prepareDatabase()
        transaction {
            SchemaUtils.create(EventData)
            SchemaUtils.create(EventType)
            SchemaUtils.create(Event)
            addRecords {
                EventData.insert {
                    it[id] = 2
                    it[hash] = 2L
                    it[sharedData] = "This is sharedData, exclude"
                }
                EventData.insert {
                    it[id] = 7
                    it[hash] = 2L
                    it[sharedData] = "This is sharedData"
                }
                EventType.insert {
                    it[id] = 3
                    it[type] = "Type of eventType"
                }
                Event.insert {
                    it[id] = 1
                    it[type] = "T"
                    it[data] = "D"
                    it[origin] = "O"
                    it[originIdx] = 1
                    it[timestamp] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[timestampTs] = kInstant.fromEpochSeconds(1)
                    it[contextId] = "C"
                    it[contextUserId] = "U"
                    it[contextParentId] = "P"
                    it[dataId] = 2
                    it[contextIdBinary] = ByteArray(0)
                    it[contextUserIdBinary] = ByteArray(0)
                    it[contextParentIdBinary] = ByteArray(0)
                    it[eventTypeId] = 3
                }
                Event.insert {
                    it[id] = 6
                    it[type] = "T"
                    it[data] = "D"
                    it[origin] = "O"
                    it[originIdx] = 0
                    it[timestamp] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[timestampTs] = kInstant.fromEpochSeconds(1)
                    it[contextId] = "C"
                    it[contextUserId] = "U"
                    it[contextParentId] = "P"
                    it[dataId] = 7
                    it[contextIdBinary] = ByteArray(0)
                    it[contextUserIdBinary] = ByteArray(0)
                    it[contextParentIdBinary] = ByteArray(0)
                    it[eventTypeId] = null
                }
            }
        }
    }
}