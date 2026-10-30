package com.anjo.monhomeass.publish

import com.anjo.monhomeass.repository.StatisticRepo
import io.nats.client.JetStream
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamManagement
import io.nats.client.PublishOptions
import io.nats.client.api.StreamConfiguration
import kotlinx.serialization.json.Json
import java.time.Duration

// Pushes HA short-term statistics (5 min) to a NATS JetStream subject as
// StatisticsService native records, one message per row. The HA row id is the
// Nats-Msg-Id: JetStream dedups resends, and the stream's last message says
// where to resume after a restart — no state of our own, the DB stays read-only.
class StatisticPublisher(
        private val repo: StatisticRepo,
        private val js: JetStream,
        private val jsm: JetStreamManagement,
        private val stream: String,
        private val subject: String
) {

    // ponytail: created only when missing, so a stream defined by the broker's
    // own config wins. 30 days max age = how long StatisticsService may lag.
    fun ensureStream() {
        try {
            jsm.getStreamInfo(stream)
        } catch (e: JetStreamApiException) {
            if (e.errorCode != NOT_FOUND) throw e
            jsm.addStream(StreamConfiguration.builder().name(stream).subjects(subject).maxAge(Duration.ofDays(30)).build())
        }
    }

    fun lastPublishedId(): Long {
        return try {
            jsm.getLastMessage(stream, subject).headers?.getFirst(MSG_ID_HEADER)?.toLongOrNull() ?: 0
        } catch (e: JetStreamApiException) {
            if (e.errorCode == NOT_FOUND) 0 else throw e
        }
    }

    // Returns how many records were published.
    fun publishNew(batchSize: Int = 500): Int {
        var lastId = lastPublishedId()
        var published = 0
        while (true) {
            val batch = repo.getShortTermAfter(lastId, batchSize)
            batch.forEach { (id, record) ->
                val options = PublishOptions.builder().messageId(id.toString()).build()
                js.publish(subject, Json.encodeToString(record).toByteArray(), options)
                lastId = id
            }
            published += batch.size
            if (batch.size < batchSize) return published
        }
    }

    private companion object {
        const val NOT_FOUND = 404
        const val MSG_ID_HEADER = "Nats-Msg-Id"
    }
}
