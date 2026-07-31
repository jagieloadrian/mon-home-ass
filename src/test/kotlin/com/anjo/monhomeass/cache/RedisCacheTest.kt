package com.anjo.monhomeass.cache

import com.anjo.monhomeass.testutils.flushRedis
import com.anjo.monhomeass.testutils.testRedisHostPort
import io.kotest.matchers.shouldBe
import redis.clients.jedis.RedisClient
import kotlin.test.Test
import kotlin.test.assertNull

class RedisCacheTest {

    private fun newCache(ttlMillis: Long): RedisCache {
        flushRedis()
        val (host, port) = testRedisHostPort()
        return RedisCache(RedisClient.create(host, port), ttlMillis)
    }

    @Test
    fun `given cached key when getOrPut repeated then loader not invoked again`() {
        val cache = newCache(ttlMillis = 60_000)
        var loaderCalls = 0

        val first = cache.getOrPut("k1") { loaderCalls++; "value" }
        val second = cache.getOrPut("k1") { loaderCalls++; "value-should-not-be-used" }

        first shouldBe "value"
        second shouldBe "value"
        loaderCalls shouldBe 1
    }

    @Test
    fun `given different keys when getOrPut then each is isolated`() {
        val cache = newCache(ttlMillis = 60_000)

        val a = cache.getOrPut("key-a") { "value-a" }
        val b = cache.getOrPut("key-b") { "value-b" }

        a shouldBe "value-a"
        b shouldBe "value-b"
    }

    @Test
    fun `given expired ttl when getOrPut then loader invoked again`() {
        val cache = newCache(ttlMillis = 50)
        var loaderCalls = 0

        cache.getOrPut("k2") { loaderCalls++; "first" }
        Thread.sleep(150)
        val afterExpiry = cache.getOrPut("k2") { loaderCalls++; "second" }

        afterExpiry shouldBe "second"
        loaderCalls shouldBe 2
    }

    @Test
    fun `given loader returns null when getOrPut then result not cached`() {
        val cache = newCache(ttlMillis = 60_000)
        var loaderCalls = 0

        val first = cache.getOrPut<String>("k3") { loaderCalls++; null }
        val second = cache.getOrPut<String>("k3") { loaderCalls++; null }

        assertNull(first)
        assertNull(second)
        loaderCalls shouldBe 2
    }
}
