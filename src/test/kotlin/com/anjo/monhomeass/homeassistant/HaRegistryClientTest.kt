package com.anjo.monhomeass.homeassistant

import com.anjo.monhomeass.model.dto.HaRegistries
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

private const val TOKEN = "t0k3n"

private val REGISTRY_RESULTS =
    mapOf(
        "config/entity_registry/list" to
            """[{"entity_id":"sensor.kitchen_co2","name":null,"original_name":"CO2","has_entity_name":true,""" +
            """"device_id":"dev1","area_id":null,"entity_category":"diagnostic","platform":"zha"}]""",
        "config/device_registry/list" to
            """[{"id":"dev1","name":"Air sensor","name_by_user":"Air","area_id":"kitchen","manufacturer":"IKEA"}]""",
        "config/area_registry/list" to """[{"area_id":"kitchen","name":"Kitchen","floor_id":"ground","icon":null}]""",
        "config/floor_registry/list" to """[{"floor_id":"ground","name":"Ground floor","level":0}]""",
    )

private val EXPECTED_REGISTRIES =
    HaRegistries(
        entities =
            listOf(
                HaRegistries.Entity(
                    entityId = "sensor.kitchen_co2",
                    originalName = "CO2",
                    hasEntityName = true,
                    deviceId = "dev1",
                    entityCategory = "diagnostic",
                ),
            ),
        devices = listOf(HaRegistries.Device(id = "dev1", nameByUser = "Air", areaId = "kitchen")),
        areas = listOf(HaRegistries.Area(areaId = "kitchen", name = "Kitchen", floorId = "ground")),
        floors = listOf(HaRegistries.Floor(floorId = "ground", name = "Ground floor")),
    )

class HaRegistryClientTest :
    StringSpec({

        suspend fun DefaultWebSocketServerSession.receiveJson(): JsonObject =
            Json.parseToJsonElement((incoming.receive() as Frame.Text).readText()).jsonObject

        fun ApplicationTestBuilder.fakeHomeAssistant(session: suspend DefaultWebSocketServerSession.() -> Unit): HaRegistryClient {
            environment { config = MapApplicationConfig() }
            install(WebSockets)
            routing { webSocket("/api/websocket") { session() } }
            return HaRegistryClient(createClient { install(ClientWebSockets) }, "http://localhost/", TOKEN)
        }

        "should fetch the four registries when the token is accepted and results arrive out of order" {
            testApplication {
                // given
                val received = mutableListOf<JsonObject>()
                val client =
                    fakeHomeAssistant {
                        send("""{"type":"auth_required","ha_version":"2026.11.0"}""")
                        received += receiveJson()
                        send("""{"type":"auth_ok","ha_version":"2026.11.0"}""")
                        val commands = List(4) { receiveJson() }
                        received += commands
                        send("""{"type":"event","event":{"event_type":"state_changed"}}""")
                        listOf(2, 0, 3, 1).map { commands[it] }.forEach { command ->
                            val type = command.getValue("type").jsonPrimitive.content
                            val id = command.getValue("id").jsonPrimitive.content
                            send("""{"id":$id,"type":"result","success":true,"result":${REGISTRY_RESULTS.getValue(type)}}""")
                        }
                    }

                // when
                val result = client.fetch()

                // then
                result shouldBe EXPECTED_REGISTRIES
                received.map { it.toString() } shouldBe
                    listOf(
                        """{"type":"auth","access_token":"$TOKEN"}""",
                        """{"id":1,"type":"config/entity_registry/list"}""",
                        """{"id":2,"type":"config/device_registry/list"}""",
                        """{"id":3,"type":"config/area_registry/list"}""",
                        """{"id":4,"type":"config/floor_registry/list"}""",
                    )
            }
        }

        "should fail when Home Assistant rejects the token" {
            testApplication {
                // given
                val client =
                    fakeHomeAssistant {
                        send("""{"type":"auth_required","ha_version":"2026.11.0"}""")
                        receiveJson()
                        send("""{"type":"auth_invalid","message":"Invalid access token or password"}""")
                    }

                // when
                val result = shouldThrow<IllegalStateException> { client.fetch() }

                // then
                result.message shouldContain "Invalid access token or password"
            }
        }

        "should fail when a registry command is not successful" {
            testApplication {
                // given
                val client =
                    fakeHomeAssistant {
                        send("""{"type":"auth_required","ha_version":"2026.11.0"}""")
                        receiveJson()
                        send("""{"type":"auth_ok","ha_version":"2026.11.0"}""")
                        repeat(4) { receiveJson() }
                        send("""{"id":1,"type":"result","success":false,"error":{"code":"unauthorized","message":"Unauthorized"}}""")
                    }

                // when
                val result = shouldThrow<IllegalStateException> { client.fetch() }

                // then
                result.message shouldContain "config/entity_registry/list"
            }
        }
    })
