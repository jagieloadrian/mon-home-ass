package com.anjo.monhomeass.integration

import com.anjo.monhomeass.model.dto.EntityDto
import com.anjo.monhomeass.model.tables.StateMetadata
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabaseMySql
import com.anjo.monhomeass.testutils.testApplicationConfigMySql
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

class MySqlConnectionKtTest :
    StringSpec({

        "given real MySQL engine when get entities then returns seeded data" {
            testApplication {
                // given
                val db = prepareDatabaseMySql()
                transaction(db) {
                    runCatching { SchemaUtils.drop(StateMetadata) }
                    SchemaUtils.create(StateMetadata)
                    addRecords {
                        StateMetadata.insert {
                            it[id] = 1
                            it[entityId] = "sensor.mysql_probe"
                        }
                    }
                }
                environment {
                    config = testApplicationConfigMySql()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/entities")

                // then
                response.status shouldBe HttpStatusCode.OK
                Json.decodeFromString<List<EntityDto>>(response.bodyAsText()) shouldBe
                    listOf(EntityDto(1, "sensor.mysql_probe"))
            }
        }
    })
