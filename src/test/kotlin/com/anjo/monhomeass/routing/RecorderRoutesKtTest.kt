package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.RecorderDto
import com.anjo.monhomeass.model.tables.Recorder
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlinx.datetime.Instant as InstantKotlin

class RecorderRoutesKtTest {

    @Test
    fun `given migration routes when get the return migration dto`() = testApplication {
        //given
        setupWithRecord()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val dateTime = InstantKotlin.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val expected = listOf(RecorderDto(1, dateTime, dateTime, true, dateTime))

        //when
        val response = client.get("/recorders/all") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val responseBody = response.bodyAsText()
        val decodedJson = Json.decodeFromString<List<RecorderDto>>(responseBody)

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    private fun setupWithRecord() {
        prepareDatabase()
        transaction {
            SchemaUtils.create(Recorder)
            addRecords {
                Recorder.insert {
                    it[this.runId] = 1
                    it[this.startTime] = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[this.endTime] = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[this.isClosedIncorrect] = true
                    it[this.createdAt] = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC)
                }
            }
        }
    }
}