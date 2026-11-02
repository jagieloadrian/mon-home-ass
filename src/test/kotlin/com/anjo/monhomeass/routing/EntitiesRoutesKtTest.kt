package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.EntityDto
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
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EntitiesRoutesKtTest :
    StringSpec({

        fun dropTableIfExists() {
            runCatching { SchemaUtils.drop(StateMetadata) }
        }

        fun setupWithRecords() {
            prepareDatabase()
            transaction {
                dropTableIfExists()
                SchemaUtils.create(StateMetadata)
                addRecords {
                    StateMetadata.insert {
                        it[id] = 1
                        it[entityId] = "sensor.living_room_temperature"
                    }
                }
                addRecords {
                    StateMetadata.insert {
                        it[id] = 2
                        it[entityId] = null
                    }
                }
            }
        }

        "given entities when get entities then returns all entries" {
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
                        EntityDto(1, "sensor.living_room_temperature"),
                        EntityDto(2, null),
                    )

                // when
                val response =
                    client.get("/entities") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<EntityDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe expected
            }
        }

        "given repeated call after direct row mutation when get entities then returns cached value" {
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
                val first = client.get("/entities")
                directUpdate("UPDATE states_meta SET entity_id = 'changed' WHERE metadata_id = 1")
                val second = client.get("/entities")

                // then
                first.status shouldBe HttpStatusCode.OK
                second.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<List<EntityDto>>(second.bodyAsText()) shouldBe
                    Json.decodeFromString<List<EntityDto>>(first.bodyAsText())
            }
        }

        "given no entities when get entities then returns empty list" {
            testApplication {
                // given
                prepareDatabase()
                transaction {
                    dropTableIfExists()
                    SchemaUtils.create(StateMetadata)
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
                    client.get("/entities") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val decodedJson = Json.decodeFromString<List<EntityDto>>(response.bodyAsText())

                // then
                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe emptyList()
            }
        }

        "given recorder database query fails when get entities then returns 503" {
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
                    client.get("/entities") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }

                // then
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
