package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.repository.EventRepo
import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.repository.SensorRepo
import com.anjo.monhomeass.repository.StateMetadataRepo
import com.anjo.monhomeass.repository.StateRepo
import com.anjo.monhomeass.repository.StatisticMetaRepo
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.repository.StatisticRunRepo
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.DependencyKey
import io.ktor.server.plugins.di.dependencies
import redis.clients.jedis.RedisClient

fun Application.configureDI() {
    val redisConfig = environment.config.config("ktor.redis")
    val redisHost = redisConfig.propertyOrNull("host")?.getString() ?: "localhost"
    val redisPort = redisConfig.propertyOrNull("port")?.getString()?.toIntOrNull() ?: 6379
    val redisTtlMillis = redisConfig.propertyOrNull("ttlMillis")?.getString()?.toLongOrNull() ?: 300_000L

    dependencies {
        provide<EventRepo>(EventRepo::class)
        provide<MigrationRepo>(MigrationRepo::class)
        provide<RecorderRepo>(RecorderRepo::class)
        provide<SchemaChangesRepo>(SchemaChangesRepo::class)
        provide { RedisCache(RedisClient.create(redisHost, redisPort), redisTtlMillis) }
        provide { StateMetadataRepo(get(DependencyKey<RedisCache>())) }
        provide { StatisticMetaRepo(get(DependencyKey<RedisCache>())) }
        provide { StatisticRepo(get(DependencyKey<RedisCache>())) }
        provide { StateRepo(get(DependencyKey<RedisCache>())) }
        provide { StatisticRunRepo(get(DependencyKey<RedisCache>())) }
        provide { SensorRepo(get(DependencyKey<RedisCache>())) }
    }
}
