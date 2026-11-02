package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.SchemaChangeDto
import com.anjo.monhomeass.model.tables.SchemaChanges
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant

class SchemaChangesRoutesKtTest :
    StringSpec({

        fun setupWithInsert() {
            prepareDatabase()
            transaction {
                SchemaUtils.create(SchemaChanges)
                addRecords {
                    SchemaChanges.insert {
                        it[id] = 1
                        it[version] = 1
                        it[changedAt] = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1), ZoneOffset.UTC)
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
                val date = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
                val expected = listOf(SchemaChangeDto(1, 1, date))

                val response =
                    client.get("/schemas") {
                        header(HttpHeaders.ContentType, "application/json")
                        header(HttpHeaders.Accept, "application/json")
                    }
                val responseBody = response.bodyAsText()
                val decodedJson = Json.decodeFromString<List<SchemaChangeDto>>(responseBody)

                response.status shouldBe HttpStatusCode.OK
                decodedJson shouldBe expected
            }
        }

        "given recorder database query fails when get schemas then returns 503" {
            testApplication {
                prepareDatabase()
                transaction {
                    runCatching { SchemaUtils.drop(SchemaChanges) }
                }
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/schemas")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
