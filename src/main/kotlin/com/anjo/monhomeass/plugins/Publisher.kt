package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.publish.JetStreamSink
import com.anjo.monhomeass.publish.KafkaSink
import com.anjo.monhomeass.publish.Sink
import com.anjo.monhomeass.publish.StatisticPublisher
import com.anjo.monhomeass.repository.SensorRepo
import com.anjo.monhomeass.repository.StatisticRepo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun Application.configurePublisher() {
    val type = environment.config.propertyOrNull("ktor.publisher.type")?.getString() ?: "none"
    if (type == "none") return logger.info { "PUBLISHER=none, statistics publisher off" }
    val config = environment.config.config("ktor.publisher")
    require(type in setOf("jetstream", "kafka")) { "PUBLISHER must be none, jetstream or kafka, not '$type'" }
    val token = config.propertyOrNull("source_token")?.getString()
    if (token.isNullOrBlank()) return logger.error { "PUBLISHER=$type but SOURCE_TOKEN is not set, statistics publisher off" }
    val sourceId = config.string("source_id")
    val interval = config.string("interval_seconds").toLong().seconds
    val since = Clock.System.now() - config.string("lookback_minutes").toLong().minutes
    val statisticRepo: StatisticRepo by dependencies
    val sensorRepo: SensorRepo by dependencies

    launch(Dispatchers.IO) {
        val sink =
            runCatching { newSink(type, sourceId, "Bearer $token") }
                .getOrElse { return@launch logger.error(it) { "Cannot create the $type client, statistics publisher off" } }
        monitor.subscribe(ApplicationStopping) { sink.close() }
        val publisher = StatisticPublisher(statisticRepo, sensorRepo, sink, since)
        logger.info { "Publishing statistics since $since to $type as source '$sourceId'" }
        while (isActive) {
            runCatching { publisher.publishNew() }
                .onSuccess { if (it > 0) logger.info { "Published $it statistics to $type" } }
                .onFailure {
                    ensureActive()
                    logger.warn(it) { "Publishing statistics failed, retry in $interval" }
                }
            delay(interval)
        }
    }
}

private fun Application.newSink(
    type: String,
    sourceId: String,
    authorization: String,
): Sink =
    if (type == "jetstream") {
        val nats = environment.config.config("ktor.nats")
        JetStreamSink(
            url = nats.string("url"),
            user = nats.propertyOrNull("user")?.getString(),
            password = nats.propertyOrNull("password")?.getString(),
            inboxPrefix = nats.string("inbox_prefix"),
            subject = "${nats.string("subject_prefix")}.$sourceId",
            authorization = authorization,
        )
    } else {
        val kafka = environment.config.config("ktor.kafka")
        KafkaSink(kafka.string("bootstrap_servers"), kafka.string("topic"), sourceId, authorization)
    }

private fun ApplicationConfig.string(key: String) = property(key).getString()
