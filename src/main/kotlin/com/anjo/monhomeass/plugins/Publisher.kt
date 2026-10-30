package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.publish.StatisticPublisher
import com.anjo.monhomeass.repository.StatisticRepo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun Application.configurePublisher() {
    val url = environment.config.propertyOrNull("ktor.nats.url")?.getString()
    if (url.isNullOrBlank()) return logger.info { "ktor.nats.url not set, statistics publisher off" }
    val natsConfig = environment.config.config("ktor.nats")
    val stream = natsConfig.property("stream").getString()
    val subject = natsConfig.property("subject").getString()
    val interval = natsConfig.property("interval_seconds").getString().toLong().seconds
    val statisticRepo: StatisticRepo by dependencies

    launch(Dispatchers.IO) {
        // Blocks until the broker is reachable, then reconnects on its own.
        val connection = Nats.connectReconnectOnConnect(Options.builder().server(url).maxReconnects(-1).build())
        monitor.subscribe(ApplicationStopping) { connection.close() }
        val publisher = StatisticPublisher(statisticRepo, connection.jetStream(), connection.jetStreamManagement(), stream, subject)
        while (isActive) {
            runCatching {
                publisher.ensureStream()
                publisher.publishNew()
            }
                    .onSuccess { if (it > 0) logger.info { "Published $it statistics to $subject" } }
                    .onFailure { logger.warn(it) { "Publishing statistics failed, retry in $interval" } }
            delay(interval)
        }
    }
}
