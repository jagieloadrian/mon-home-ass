package com.anjo.monhomeass.publish

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.api.StreamConfiguration
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.Transferable
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

private val NATS_CONFIG =
    """
    jetstream { store_dir: /tmp/js }
    authorization {
      users: [
        { user: admin, password: admin }
        {
          user: mon-home-ass, password: secret,
          permissions: { publish: ["stats.ingest.home-assistant"], subscribe: ["_INBOX_mon_home_ass.>"] }
        }
      ]
    }
    """.trimIndent()

class SinksTest :
    StringSpec({

        "given the homelab NATS permissions when JetStreamSink sends then the stream holds the payload with the source token" {
            // given
            val nats =
                GenericContainer<Nothing>(DockerImageName.parse("nats:2.15.2")).apply {
                    withCopyToContainer(Transferable.of(NATS_CONFIG), "/etc/nats/test.conf")
                    withCommand("-c", "/etc/nats/test.conf")
                    withExposedPorts(4222)
                    start()
                }
            val url = "nats://${nats.host}:${nats.getMappedPort(4222)}"
            val admin =
                Nats.connect(
                    Options
                        .builder()
                        .server(url)
                        .userInfo("admin", "admin")
                        .build(),
                )
            val jsm = admin.jetStreamManagement()
            jsm.addStream(
                StreamConfiguration
                    .builder()
                    .name("STATS_INGEST")
                    .subjects("stats.ingest.>")
                    .build(),
            )

            // when
            JetStreamSink(url, "mon-home-ass", "secret", "_INBOX_mon_home_ass", "stats.ingest.home-assistant", "Bearer t0k3n").use {
                it.send("""{"ts":"2026-11-03T10:00:00Z"}""".toByteArray())
            }

            // then
            val message = jsm.getMessage("STATS_INGEST", 1)
            message.subject shouldBe "stats.ingest.home-assistant"
            message.headers.shouldNotBeNull().getFirst("Authorization") shouldBe "Bearer t0k3n"
            message.data.shouldNotBeNull().decodeToString() shouldBe """{"ts":"2026-11-03T10:00:00Z"}"""
            admin.close()
            nats.stop()
        }

        "given a Kafka broker when KafkaSink sends then the topic holds the payload keyed by the source id with the source token" {
            // given
            val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:4.4.0")).apply { start() }

            // when
            KafkaSink(kafka.bootstrapServers, "stats.ingest", "home-assistant", "Bearer t0k3n").use {
                it.send("""{"ts":"2026-11-03T10:00:00Z"}""".toByteArray())
            }

            // then
            val consumer =
                KafkaConsumer(
                    mapOf<String, Any>(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG to "test",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ),
                    StringDeserializer(),
                    ByteArrayDeserializer(),
                )
            consumer.subscribe(listOf("stats.ingest"))
            val record = generateSequence { consumer.poll(Duration.ofSeconds(1)) }.take(30).first { !it.isEmpty }.single()
            record.key() shouldBe "home-assistant"
            record
                .headers()
                .lastHeader("Authorization")
                .value()
                .decodeToString() shouldBe "Bearer t0k3n"
            record.value().decodeToString() shouldBe """{"ts":"2026-11-03T10:00:00Z"}"""
            consumer.close()
            kafka.stop()
        }
    })
