package com.anjo.monhomeass.cache

import com.anjo.monhomeass.testutils.flushRedis
import com.anjo.monhomeass.testutils.testRedisHostPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import redis.clients.jedis.RedisClient
import redis.clients.jedis.exceptions.JedisConnectionException

class RedisCacheTest : StringSpec({

    fun newCache(ttlMillis: Long): RedisCache {
        flushRedis()
        val (host, port) = testRedisHostPort()
        return RedisCache(RedisClient.create(host, port), ttlMillis)
    }

    "given cached key when getOrPut repeated then loader not invoked again" {
        val cache = newCache(ttlMillis = 60_000)
        var loaderCalls = 0

        val first = cache.getOrPut("k1") { loaderCalls++; "value" }
        val second = cache.getOrPut("k1") { loaderCalls++; "value-should-not-be-used" }

        first shouldBe "value"
        second shouldBe "value"
        loaderCalls shouldBe 1
    }

    "given different keys when getOrPut then each is isolated" {
        val cache = newCache(ttlMillis = 60_000)

        val a = cache.getOrPut("key-a") { "value-a" }
        val b = cache.getOrPut("key-b") { "value-b" }

        a shouldBe "value-a"
        b shouldBe "value-b"
    }

    "given expired ttl when getOrPut then loader invoked again" {
        val cache = newCache(ttlMillis = 50)
        var loaderCalls = 0

        cache.getOrPut("k2") { loaderCalls++; "first" }
        Thread.sleep(150)
        val afterExpiry = cache.getOrPut("k2") { loaderCalls++; "second" }

        afterExpiry shouldBe "second"
        loaderCalls shouldBe 2
    }

    "given loader returns null when getOrPut then result not cached" {
        val cache = newCache(ttlMillis = 60_000)
        var loaderCalls = 0

        val first = cache.getOrPut<String>("k3") { loaderCalls++; null }
        val second = cache.getOrPut<String>("k3") { loaderCalls++; null }

        first.shouldBeNull()
        second.shouldBeNull()
        loaderCalls shouldBe 2
    }

    "given redis unreachable when getOrPut then falls back to in-memory cache and retries with backoff" {
        val client = mockk<RedisClient>()
        every { client.get(any<String>()) } throws JedisConnectionException("down")
        val cache = RedisCache(client, ttlMillis = 60_000)
        var loaderCalls = 0

        val first = cache.getOrPut("k") { loaderCalls++; "value" }
        cache.down shouldBe true
        val backoffAfterFirstFailure = cache.backoffMillis

        // still within backoff window: served from local cache, no retry against redis
        val second = cache.getOrPut("k") { loaderCalls++; "should-not-be-used" }

        first shouldBe "value"
        second shouldBe "value"
        loaderCalls shouldBe 1

        // force the backoff window to have elapsed: retries redis, fails again, backoff doubles
        cache.nextCheckAt = 0
        cache.getOrPut("k") { loaderCalls++; "value" }
        cache.backoffMillis shouldBe backoffAfterFirstFailure * 2

        // redis recovers: next retry succeeds and clears the local fallback state
        every { client.get(any<String>()) } returns null
        every { client.set(any<String>(), any<String>(), any()) } returns "OK"
        cache.nextCheckAt = 0
        val afterRecovery = cache.getOrPut("k") { loaderCalls++; "fresh" }

        afterRecovery shouldBe "fresh"
        cache.down shouldBe false
    }
})
