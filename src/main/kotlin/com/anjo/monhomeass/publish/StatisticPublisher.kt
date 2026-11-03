package com.anjo.monhomeass.publish

import com.anjo.monhomeass.repository.StatisticRepo
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class StatisticPublisher(
    private val repo: StatisticRepo,
    private val sink: Sink,
    private val since: Instant,
    private val batchSize: Int = 500,
) {
    private var lastId = 0L

    fun publishNew(): Int {
        var published = 0
        while (true) {
            val batch = repo.getShortTermAfter(lastId, since, batchSize)
            batch.forEach { (id, record) ->
                sink.send(Json.encodeToString(record).toByteArray())
                lastId = id
                published++
            }
            if (batch.size < batchSize) return published
        }
    }
}
