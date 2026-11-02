package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.MigrationDto
import com.anjo.monhomeass.model.tables.MigrationChanges
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
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

class MigrationRoutesKtTest :
    StringSpec({

        fun setupWithInsert() {
            prepareDatabase()
            transaction {
                SchemaUtils.create(MigrationChanges)
                addRecords {
                    MigrationChanges.insert {
                        it[id] = "new_id"
                        it[version] = 1
                    }
                }
            }
        }

        "given migration routes when get the return migration dto" {
            testApplication {
                setupWithInsert()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureRouting()
                }
                val expected = listOf(MigrationDto("new_id", 1))

                val response =
                    client.get("/migrations") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val responseBody = response.bodyAsText()
                val decodedJson = Json.decodeFromString<List<MigrationDto>>(responseBody)

                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe expected
            }
        }

        "given recorder database query fails when get migrations then returns 503" {
            testApplication {
                prepareDatabase()
                transaction {
                    runCatching { SchemaUtils.drop(MigrationChanges) }
                }
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/migrations")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
