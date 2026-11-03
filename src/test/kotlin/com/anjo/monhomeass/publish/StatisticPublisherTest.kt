package com.anjo.monhomeass.publish

import com.anjo.monhomeass.model.dto.StatisticRecordDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

private val SINCE = Instant.fromEpochSeconds(1_700_000_000L)

class StatisticPublisherTest :
    StringSpec({

        class RecordingSink : Sink {
            val records = mutableListOf<StatisticRecordDto>()

            override fun send(payload: ByteArray) {
                records += Json.decodeFromString<StatisticRecordDto>(payload.decodeToString())
            }

            override fun close() = Unit
        }

        fun addShortTerm(
            rowId: Long,
            startSeconds: Long,
            rowMean: Double?,
        ) {
            addRecords {
                StatisticShortTerm.insert {
                    it[id] = rowId
                    it[metadataId] = 1
                    it[startTs] = Instant.fromEpochSeconds(startSeconds)
                    it[mean] = rowMean
                    it[min] = 5.0
                    it[max] = 15.0
                }
            }
        }

        fun setupWithRecords() {
            prepareDatabase()
            transaction {
                runCatching { SchemaUtils.drop(Statistic) }
                runCatching { SchemaUtils.drop(StatisticShortTerm) }
                runCatching { SchemaUtils.drop(StatisticMetaData) }
                SchemaUtils.create(StatisticMetaData, StatisticShortTerm)
                addRecords {
                    StatisticMetaData.insert {
                        it[id] = 1
                        it[statisticId] = "sensor.living_room_temperature"
                        it[meanType] = 0
                    }
                }
            }
            addShortTerm(1, 1_699_999_700L, 9.0)
            addShortTerm(2, 1_700_000_000L, 10.0)
            addShortTerm(3, 1_700_000_300L, null)
        }

        "given rows since the lookback start when publishNew then sends each once and later only new rows" {
            // given
            setupWithRecords()
            val sink = RecordingSink()
            val publisher = StatisticPublisher(StatisticRepo(mockk()), sink, SINCE, batchSize = 1)

            // when
            val first = publisher.publishNew()
            val again = publisher.publishNew()
            addShortTerm(4, 1_700_000_600L, 12.0)
            val afterNewRow = publisher.publishNew()

            // then
            first shouldBe 2
            again shouldBe 0
            afterNewRow shouldBe 1
            sink.records.map { it.ts } shouldBe listOf("2023-11-14T22:13:20Z", "2023-11-14T22:18:20Z", "2023-11-14T22:23:20Z")
            sink.records.first() shouldBe
                StatisticRecordDto(
                    ts = "2023-11-14T22:13:20Z",
                    dims = mapOf("statistic_id" to "sensor.living_room_temperature"),
                    vals = mapOf("mean" to 10.0, "min" to 5.0, "max" to 15.0),
                )
            sink.records[1].vals shouldBe mapOf("min" to 5.0, "max" to 15.0)
        }

        "given a send failing midway when publishNew then the next call resumes at the failed row" {
            // given
            setupWithRecords()
            var sent = 0
            val sink = RecordingSink()
            val flaky =
                object : Sink by sink {
                    override fun send(payload: ByteArray) {
                        if (sent++ == 1) error("broker down")
                        sink.send(payload)
                    }
                }
            val publisher = StatisticPublisher(StatisticRepo(mockk()), flaky, SINCE)

            // when
            val failed = runCatching { publisher.publishNew() }
            val published = publisher.publishNew()

            // then
            failed.isFailure shouldBe true
            published shouldBe 1
            sink.records.map { it.ts } shouldBe listOf("2023-11-14T22:13:20Z", "2023-11-14T22:18:20Z")
        }
    })
