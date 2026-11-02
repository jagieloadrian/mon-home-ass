package com.anjo.monhomeass.integration

import com.anjo.monhomeass.model.dto.EntityDto
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.seedSqlite
import com.anjo.monhomeass.testutils.testApplicationConfigSqlite
import io.kotest.assertions.throwables.shouldThrow
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

class SqliteConnectionKtTest :
    StringSpec({

        "given real SQLite file when get entities then returns seeded data via readonly connection and reports ready" {
            testApplication {
                // given
                val config = testApplicationConfigSqlite()
                val filePath = config.property("ktor.database.file_path").getString()
                seedSqlite(filePath) {
                    SchemaUtils.create(StateMetadata)
                    StateMetadata.insert {
                        it[id] = 1
                        it[entityId] = "sensor.sqlite_probe"
                    }
                }
                environment {
                    this.config = config
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/entities")
                val ready = client.get("/health/ready")

                // then
                response.status shouldBe HttpStatusCode.OK
                ready.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<List<EntityDto>>(response.bodyAsText()) shouldBe
                    listOf(EntityDto(1, "sensor.sqlite_probe"))

                shouldThrow<Exception> {
                    transaction {
                        StateMetadata.insert {
                            it[id] = 2
                            it[entityId] = "sensor.should_not_write"
                        }
                    }
                }
            }
        }
    })
