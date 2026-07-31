package com.anjo.monhomeass.cache

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPooled

// ponytail: no eviction cap beyond TTL — Redis itself can be given a
// maxmemory/eviction policy at the infra level if this ever matters; add
// app-side eviction logic only if that's not enough in practice.
// ponytail: no Redis-down fallback — if Redis is unreachable, getOrPut
// throws and the existing StatusPages exception handler turns that into a
// 503, same as a DB failure. Add a fallback-to-loader() wrap only if Redis
// downtime is observed to actually break previously-DB-only-dependent reads.
class RedisCache(@PublishedApi internal val client: JedisPooled, @PublishedApi internal val ttlMillis: Long) {

    inline fun <reified V> getOrPut(key: String, loader: () -> V?): V? {
        client.get(key)?.let { return Json.decodeFromString<V>(it) }
        val value = loader() ?: return null
        client.psetex(key, ttlMillis, Json.encodeToString(value))
        return value
    }
}
