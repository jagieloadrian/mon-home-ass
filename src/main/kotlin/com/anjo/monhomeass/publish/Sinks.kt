package com.anjo.monhomeass.publish

import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.impl.Headers
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer

private const val AUTHORIZATION = "Authorization"

interface Sink : AutoCloseable {
    fun send(payload: ByteArray)
}

class JetStreamSink(
    url: String,
    user: String?,
    password: String?,
    inboxPrefix: String,
    private val subject: String,
    authorization: String,
) : Sink {
    private val connection =
        Nats.connectReconnectOnConnect(
            Options
                .builder()
                .server(url)
                .inboxPrefix(inboxPrefix)
                .maxReconnects(-1)
                .apply { if (!user.isNullOrBlank()) userInfo(user, password.orEmpty()) }
                .build(),
        )
    private val jetStream = connection.jetStream()
    private val headers = Headers().add(AUTHORIZATION, authorization)

    override fun send(payload: ByteArray) {
        jetStream.publish(subject, headers, payload)
    }

    override fun close() = connection.close()
}

class KafkaSink(
    bootstrapServers: String,
    private val topic: String,
    private val key: String,
    authorization: String,
) : Sink {
    private val producer =
        KafkaProducer(
            mapOf<String, Any>(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers),
            StringSerializer(),
            ByteArraySerializer(),
        )
    private val headers = listOf(RecordHeader(AUTHORIZATION, authorization.toByteArray()))

    override fun send(payload: ByteArray) {
        producer.send(ProducerRecord(topic, null, key, payload, headers)).get()
    }

    override fun close() = producer.close()
}
