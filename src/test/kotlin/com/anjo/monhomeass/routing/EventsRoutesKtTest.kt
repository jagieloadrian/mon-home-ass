package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.DataContentDto
import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.model.dto.EventTypeDto
import com.anjo.monhomeass.model.dto.toEventDto
import com.anjo.monhomeass.model.tables.Event
import com.anjo.monhomeass.model.tables.EventData
import com.anjo.monhomeass.model.tables.EventType
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant
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

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val dateTime = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val eventDto = EventDto(1, "T", "D", "O", 1, dateTime, Instant.fromEpochSeconds(1),
                "C", "U", "P", DataContentDto(2, 2L, "This is sharedData, exclude something"),
                EventTypeDto(3, "Type of eventType"))
        val expected = listOf(eventDto)

        //when and then
       client.get("/events") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }.apply {
            status shouldBe HttpStatusCode.OK
            body<List<EventDto>>() shouldBe expected
        }
    }

    @Test
    fun `given event routes with exclude when get then return empty list`() = testApplication {
        //given
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        //when and then
        client.get("/events") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            parameter("exclude", "exclude")
        }.apply {
            status shouldBe HttpStatusCode.OK
            body<List<EventDto>>() shouldBe emptyList()
        }
    }

    private fun setupWithRecord() {
        prepareDatabase()
        transaction {
            SchemaUtils.create(EventData)
            SchemaUtils.create(EventType)
            SchemaUtils.create(Event)
            EventData.insert {
                it[id] = 2
                it[hash] = 2L
                it[sharedData] = "This is sharedData, exclude something"
            }
            EventType.insert {
                it[id] = 3
                it[type] = "Type of eventType"
            }
            EventData.insert {
                it[id] = 7
                it[hash] = 4L
                it[sharedData] = "This is sharedData"
            }
            Event.insert {
                it[id] = 1
                it[type] = "T"
                it[data] = "D"
                it[origin] = "O"
                it[originIdx] = 1
                it[timestamp] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                it[timestampTs] = Instant.fromEpochSeconds(1)
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
                it[timestampTs] = Instant.fromEpochSeconds(1)
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
