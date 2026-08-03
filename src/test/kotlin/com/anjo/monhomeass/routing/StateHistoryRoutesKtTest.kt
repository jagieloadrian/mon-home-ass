package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StateDto
import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class StateHistoryRoutesKtTest : StringSpec({

    fun dropTablesIfExists() {
        runCatching { SchemaUtils.drop(State) }
        runCatching { SchemaUtils.drop(StateAttributes) }
        runCatching { SchemaUtils.drop(StateMetadata) }
    }

    fun setupWithRecords() {
        prepareDatabase()
        transaction {
            dropTablesIfExists()
            SchemaUtils.create(StateMetadata, StateAttributes, State)
            addRecords {
                StateMetadata.insert {
                    it[id] = 1
                    it[entityId] = "sensor.living_room_temperature"
                }
            }
            addRecords {
                State.insert {
                    it[id] = 1
                    it[metadataId] = 1
                    it[state] = "20.0"
                    it[lastUpdatedTs] = Instant.fromEpochSeconds(1_700_000_000L)
                    it[lastModifiedTs] = Instant.fromEpochSeconds(1_700_000_000L)
                }
            }
            addRecords {
                State.insert {
                    it[id] = 2
                    it[metadataId] = 1
                    it[state] = "21.5"
                    it[lastUpdatedTs] = Instant.fromEpochSeconds(1_700_003_600L)
                    it[lastModifiedTs] = Instant.fromEpochSeconds(1_700_003_600L)
                }
            }
        }
    }

    "given known entityId when get history then returns all rows oldest first" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history")
            val decoded = Json.decodeFromString<List<StateDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decoded shouldBe listOf(
                    StateDto("sensor.living_room_temperature", "20.0", null, 1_700_000_000L, 1_700_000_000L),
                    StateDto("sensor.living_room_temperature", "21.5", null, 1_700_003_600L, 1_700_003_600L)
            )
        }
    }

    "given limit and offset when get history then returns next page" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?limit=1&offset=1")
            val decoded = Json.decodeFromString<List<StateDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decoded shouldBe listOf(
                    StateDto("sensor.living_room_temperature", "21.5", null, 1_700_003_600L, 1_700_003_600L)
            )
        }
    }

    "given from and to query params when get history then returns only in-range rows" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?from=1700003000&to=1700004000")
            val decoded = Json.decodeFromString<List<StateDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decoded shouldBe listOf(
                    StateDto("sensor.living_room_temperature", "21.5", null, 1_700_003_600L, 1_700_003_600L)
            )
        }
    }

    "given unknown entityId when get history then returns 404" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.does_not_exist/history")

            //then
            response.status shouldBe HttpStatusCode.NotFound
            Json.decodeFromString<ErrorDto>(response.bodyAsText()) shouldBe ErrorDto("Unknown entityId")
        }
    }

    "given known entityId with no rows in range when get history then returns 200 empty list" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?from=1600000000&to=1600000001")
            val decoded = Json.decodeFromString<List<StateDto>>(response.bodyAsText())

            //then
            response.status shouldBe HttpStatusCode.OK
            decoded shouldBe emptyList()
        }
    }

    "given negative limit when get history then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?limit=-1")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
            Json.decodeFromString<ErrorDto>(response.bodyAsText()) shouldBe ErrorDto("Invalid 'limit' value")
        }
    }

    "given negative offset when get history then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?offset=-1")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    "given unparseable from when get history then returns 400" {
        testApplication {
            //given
            setupWithRecords()
            environment { config = testApplicationConfig() }
            application {
                configureDatabases()
                configureStatusPages()
                configureRouting()
            }

            //when
            val response = client.get("/states/sensor.living_room_temperature/history?from=not-a-date")

            //then
            response.status shouldBe HttpStatusCode.BadRequest
            Json.decodeFromString<ErrorDto>(response.bodyAsText()) shouldBe ErrorDto("Invalid 'from' value")
        }
    }
})
