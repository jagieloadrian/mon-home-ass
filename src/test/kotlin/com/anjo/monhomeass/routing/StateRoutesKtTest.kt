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

class StateRoutesKtTest :
    StringSpec({

        // ponytail: State FKs into StateMetadata and StateAttributes — drop it first
        // so each test starts clean regardless of run order.
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
                    StateMetadata.insert {
                        it[id] = 2
                        it[entityId] = "sensor.no_state_rows"
                    }
                }
                addRecords {
                    StateAttributes.insert {
                        it[id] = 1
                        it[sharedAttributes] = "{\"unit\":\"C\"}"
                    }
                }
                addRecords {
                    State.insert {
                        it[id] = 1
                        it[metadataId] = 1
                        it[attributesId] = 1
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

        "given known entityId when get state then returns latest row" {
            testApplication {
                // given
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/states/sensor.living_room_temperature")
                val decoded = Json.decodeFromString<StateDto>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decoded shouldBe
                    StateDto(
                        entityId = "sensor.living_room_temperature",
                        state = "21.5",
                        attributes = null,
                        lastChangedTs = 1_700_003_600L,
                        lastUpdatedTs = 1_700_003_600L,
                        eventId = null,
                    )
            }
        }

        "given entityId with attributes when get state then includes shared attributes" {
            testApplication {
                // given
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
                        StateAttributes.insert {
                            it[id] = 1
                            it[sharedAttributes] = "{\"unit\":\"C\"}"
                        }
                    }
                    addRecords {
                        State.insert {
                            it[id] = 1
                            it[metadataId] = 1
                            it[attributesId] = 1
                            it[state] = "20.0"
                            it[lastUpdatedTs] = Instant.fromEpochSeconds(1_700_000_000L)
                            it[lastModifiedTs] = Instant.fromEpochSeconds(1_700_000_000L)
                        }
                    }
                }
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/states/sensor.living_room_temperature")
                val decoded = Json.decodeFromString<StateDto>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decoded.attributes shouldBe "{\"unit\":\"C\"}"
            }
        }

        "given unknown entityId when get state then returns 404" {
            testApplication {
                // given
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/states/sensor.does_not_exist")

                // then
                response.status shouldBe HttpStatusCode.NotFound
                Json.decodeFromString<ErrorDto>(response.bodyAsText()) shouldBe ErrorDto("Unknown entityId")
            }
        }

        "given entityId with metadata but zero state rows when get state then returns 404" {
            testApplication {
                // given
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/states/sensor.no_state_rows")

                // then
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        "given repeated call after direct row mutation when get state then returns cached value" {
            testApplication {
                // given
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val first = client.get("/states/sensor.living_room_temperature")
                directUpdate("UPDATE states SET state = 'changed' WHERE state_id = 2")
                val second = client.get("/states/sensor.living_room_temperature")

                // then
                first.status shouldBe HttpStatusCode.OK
                second.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<StateDto>(second.bodyAsText()) shouldBe
                    Json.decodeFromString<StateDto>(first.bodyAsText())
            }
        }

        "given recorder database query fails when get state then returns 503" {
            testApplication {
                // given - table intentionally not (re)created, so the query fails
                prepareDatabase()
                transaction { dropTablesIfExists() }
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/states/sensor.living_room_temperature")

                // then
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
