package com.anjo.monhomeass.cache

import kotlinx.serialization.json.Json
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.params.SetParams
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class RedisCache(
    @PublishedApi internal val client: RedisClient,
    @PublishedApi internal val ttlMillis: Long,
) {
    @PublishedApi internal val local = ConcurrentHashMap<String, Pair<String, Long>>()

    @Volatile @PublishedApi
    internal var down = false

    @Volatile @PublishedApi
    internal var backoffMillis = INITIAL_BACKOFF_MILLIS

    @Volatile @PublishedApi
    internal var nextCheckAt = 0L

    inline fun <reified V> getOrPut(
        key: String,
        loader: () -> V?,
    ): V? {
        if (down && System.currentTimeMillis() < nextCheckAt) {
            return localGetOrPut(key, loader)
        }
        return try {
            val cached = client.get(key)
            markUp()
            if (cached != null) return Json.decodeFromString<V>(cached)
            val value = loader() ?: return null
            client.set(key, Json.encodeToString(value), SetParams().px(ttlMillis))
            value
        } catch (_: JedisConnectionException) {
            markDown()
            localGetOrPut(key, loader)
        }
    }

    @PublishedApi
    internal inline fun <reified V> localGetOrPut(
        key: String,
        loader: () -> V?,
    ): V? {
        val now = System.currentTimeMillis()
        local[key]?.let { (json, expiresAt) ->
            if (expiresAt > now) return Json.decodeFromString<V>(json)
            local.remove(key)
        }
        val value = loader() ?: return null
        local[key] = Json.encodeToString(value) to (now + ttlMillis)
        return value
    }

    @PublishedApi
    internal fun markDown() {
        backoffMillis = if (down) min(backoffMillis * 2, MAX_BACKOFF_MILLIS) else INITIAL_BACKOFF_MILLIS
        down = true
        nextCheckAt = System.currentTimeMillis() + backoffMillis
    }

    @PublishedApi
    internal fun markUp() {
        if (down) {
            down = false
            local.clear()
        }
    }

    private companion object {
        const val INITIAL_BACKOFF_MILLIS = 1_000L
        const val MAX_BACKOFF_MILLIS = 2 * 24 * 60 * 60 * 1_000L
    }
}
