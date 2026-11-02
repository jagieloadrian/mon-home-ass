package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.ErrorDto
import com.anjo.monhomeass.model.dto.StatisticRunDto
import com.anjo.monhomeass.model.tables.StatisticRun
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.addRecords
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
import java.time.OffsetDateTime
import java.time.ZoneOffset

class StatisticRunRoutesKtTest :
    StringSpec({

        fun dropTableIfExists() {
            runCatching { SchemaUtils.drop(StatisticRun) }
        }

        fun setupWithRecords() {
            prepareDatabase()
            transaction {
                dropTableIfExists()
                SchemaUtils.create(StatisticRun)
                addRecords {
                    StatisticRun.insert {
                        it[id] = 1
                        it[startedAt] = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC)
                    }
                }
                addRecords {
                    StatisticRun.insert {
                        it[id] = 2
                        it[startedAt] = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1_700_003_600L), ZoneOffset.UTC)
                    }
                }
            }
        }

        "given runs when get statistics-runs then returns most recent first" {
            testApplication {
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/statistics-runs")
                val decoded = Json.decodeFromString<List<StatisticRunDto>>(response.bodyAsText())

                response.status shouldBe HttpStatusCode.OK
                decoded shouldBe
                    listOf(
                        StatisticRunDto(2, 1_700_003_600L),
                        StatisticRunDto(1, 1_700_000_000L),
                    )
            }
        }

        "given limit when get statistics-runs then returns only first N rows" {
            testApplication {
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/statistics-runs?limit=1")
                val decoded = Json.decodeFromString<List<StatisticRunDto>>(response.bodyAsText())

                response.status shouldBe HttpStatusCode.OK
                decoded shouldBe listOf(StatisticRunDto(2, 1_700_003_600L))
            }
        }

        "given no runs when get statistics-runs then returns empty list" {
            testApplication {
                prepareDatabase()
                transaction {
                    dropTableIfExists()
                    SchemaUtils.create(StatisticRun)
                }
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/statistics-runs")
                val decoded = Json.decodeFromString<List<StatisticRunDto>>(response.bodyAsText())

                response.status shouldBe HttpStatusCode.OK
                decoded shouldBe emptyList()
            }
        }

        "given negative limit when get statistics-runs then returns 400" {
            testApplication {
                setupWithRecords()
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/statistics-runs?limit=-1")

                response.status shouldBe HttpStatusCode.BadRequest
                Json.decodeFromString<ErrorDto>(response.bodyAsText()) shouldBe ErrorDto("Invalid 'limit' value")
            }
        }

        "given recorder database query fails when get statistics-runs then returns 503" {
            testApplication {
                prepareDatabase()
                transaction { dropTableIfExists() }
                environment { config = testApplicationConfig() }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                val response = client.get("/statistics-runs")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    })
