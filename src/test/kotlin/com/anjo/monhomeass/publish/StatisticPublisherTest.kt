package com.anjo.monhomeass.publish

import com.anjo.monhomeass.model.dto.StatisticRecordDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.nats.client.Nats
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import kotlin.time.Instant

class StatisticPublisherTest :
    StringSpec({

        val nats =
            GenericContainer<Nothing>(DockerImageName.parse("nats:2-alpine")).apply {
                withCommand("-js")
                withExposedPorts(4222)
                start()
            }
        afterSpec { nats.stop() }

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
            addShortTerm(1, 1_700_000_000L, 10.0)
            addShortTerm(2, 1_700_000_300L, null)
        }

        "given new short-term rows when publishNew then publishes each once as native records and resumes after restart" {
            setupWithRecords()
            Nats.connect("nats://${nats.host}:${nats.getMappedPort(4222)}").use { connection ->
                val jsm = connection.jetStreamManagement()

                fun publisher() = StatisticPublisher(StatisticRepo(mockk()), connection.jetStream(), jsm, "HA_TEST", "ha.test.statistics")
                publisher().ensureStream()

                val first = publisher().publishNew(batchSize = 1)
                val afterRestart = publisher().publishNew()
                addShortTerm(3, 1_700_000_600L, 12.0)
                val afterNewRow = publisher().publishNew()

                first shouldBe 2
                afterRestart shouldBe 0
                afterNewRow shouldBe 1
                publisher().lastPublishedId() shouldBe 3
                jsm.getStreamInfo("HA_TEST").streamState.msgCount shouldBe 3

                fun record(seq: Long) =
                    Json.decodeFromString<StatisticRecordDto>(
                        jsm
                            .getMessage("HA_TEST", seq)
                            .data
                            .shouldNotBeNull()
                            .decodeToString(),
                    )
                record(1) shouldBe
                    StatisticRecordDto(
                        ts = "2023-11-14T22:13:20Z",
                        dims = mapOf("statistic_id" to "sensor.living_room_temperature"),
                        vals = mapOf("mean" to 10.0, "min" to 5.0, "max" to 15.0),
                    )
                record(2).vals shouldBe mapOf("min" to 5.0, "max" to 15.0)
            }
        }
    })
