package com.anjo.monhomeass.homeassistant

import com.anjo.monhomeass.model.dto.HaRegistries
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

private const val ENTITIES = "config/entity_registry/list"
private const val DEVICES = "config/device_registry/list"
private const val AREAS = "config/area_registry/list"
private const val FLOORS = "config/floor_registry/list"
private val COMMANDS = listOf(ENTITIES, DEVICES, AREAS, FLOORS)
private val TIMEOUT = 10.seconds

@OptIn(ExperimentalSerializationApi::class)
private val registryJson =
    Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

class HaRegistryClient(
    private val http: HttpClient,
    url: String,
    private val token: String,
) {
    private val endpoint = url.trimEnd('/').replaceFirst(Regex("^http"), "ws") + "/api/websocket"

    suspend fun fetch(): HaRegistries =
        withTimeoutOrNull(TIMEOUT) {
            lateinit var registries: HaRegistries
            http.webSocket(endpoint) { registries = exchange() }
            registries
        } ?: error("Home Assistant did not answer within $TIMEOUT")

    private suspend fun DefaultClientWebSocketSession.exchange(): HaRegistries {
        val results = mutableMapOf<String, JsonElement>()
        for (frame in incoming) {
            val message = (frame as? Frame.Text)?.let { Json.parseToJsonElement(it.readText()).jsonObject } ?: continue
            when (message.string("type")) {
                "auth_required" -> send(auth(token))
                "auth_ok" -> COMMANDS.forEachIndexed { index, command -> send(command(index + 1, command)) }
                "auth_invalid" -> error("Home Assistant rejected the token: ${message.string("message")}")
                "result" -> message.commandResult()?.let { (command, result) -> results[command] = result }
            }
            if (results.size == COMMANDS.size) return results.toRegistries()
        }
        error("Home Assistant closed the connection before answering")
    }

    private fun Map<String, JsonElement>.toRegistries(): HaRegistries =
        HaRegistries(
            entities = registryJson.decodeFromJsonElement(getValue(ENTITIES)),
            devices = registryJson.decodeFromJsonElement(getValue(DEVICES)),
            areas = registryJson.decodeFromJsonElement(getValue(AREAS)),
            floors = registryJson.decodeFromJsonElement(getValue(FLOORS)),
        )
}

private fun auth(token: String): String =
    buildJsonObject {
        put("type", "auth")
        put("access_token", token)
    }.toString()

private fun command(
    id: Int,
    type: String,
): String =
    buildJsonObject {
        put("id", id)
        put("type", type)
    }.toString()

private fun JsonObject.commandResult(): Pair<String, JsonElement>? {
    val command = this["id"]?.jsonPrimitive?.intOrNull?.let { COMMANDS.getOrNull(it - 1) } ?: return null
    check(this["success"]?.jsonPrimitive?.booleanOrNull == true) { "Home Assistant $command failed: ${this["error"]}" }
    return command to getValue("result")
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
