package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.SensorDto
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.plugins.configureStatusPages
import com.anjo.monhomeass.testutils.SEEDED_SENSORS
import com.anjo.monhomeass.testutils.dropSensorTables
import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.testApplicationConfig
import com.anjo.monhomeass.testutils.withSeededSensors
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SensorsRoutesKtTest :
    StringSpec({

        fun get(path: String): Pair<HttpStatusCode, String> {
            lateinit var response: Pair<HttpStatusCode, String>
            testApplication {
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }
                response = client.get(path).let { it.status to it.bodyAsText() }
            }
            return response
        }

        fun sensors(body: String) = Json.decodeFromString<List<SensorDto>>(body)

        "should list every sensor when no filter is given" {
            // given
            val database = prepareDatabase()

            // when
            val (status, body) = withSeededSensors(database) { get("/sensors") }

            // then
            status shouldBe HttpStatusCode.OK
            sensors(body) shouldBe SEEDED_SENSORS
        }

        "should list only the sensors of the category when filtered by category" {
            // given
            val database = prepareDatabase()

            // when
            val (status, body) = withSeededSensors(database) { get("/sensors?category=climate") }

            // then
            status shouldBe HttpStatusCode.OK
            sensors(body).map { it.entityId } shouldBe
                listOf("sensor.living_room_temperature", "sensor.living_room_temperature_2")
        }

        "should list no sensors when filtered by an area and Home Assistant is not configured" {
            // given
            val database = prepareDatabase()

            // when
            val (status, body) = withSeededSensors(database) { get("/sensors?area=Kitchen") }

            // then
            status shouldBe HttpStatusCode.OK
            sensors(body) shouldBe emptyList()
        }

        "should answer 503 when the recorder tables are missing" {
            // given
            transaction(prepareDatabase()) { dropSensorTables() }

            // when
            val (status, _) = get("/sensors")

            // then
            status shouldBe HttpStatusCode.ServiceUnavailable
        }
    })
