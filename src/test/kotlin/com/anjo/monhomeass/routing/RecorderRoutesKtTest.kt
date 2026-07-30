package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.RecorderDto
import com.anjo.monhomeass.model.tables.Recorder
import com.anjo.monhomeass.plugins.configureDatabases
import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.testutils.addRecords
import com.anjo.monhomeass.testutils.prepareDatabase
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant as jInstant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.time.Instant

class RecorderRoutesKtTest {

    @Test
    fun `given migration routes when get recorders the return migration dto`() = testApplication {
        //given
        setupWithRecord()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val dateTime = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val expected = listOf(RecorderDto(1, dateTime, dateTime, true, dateTime))

        //when
        val response = client.get("/recorders") {
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
                    it[runId] = 1
                    it[startTime] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[endTime] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                    it[isClosedIncorrect] = true
                    it[createdAt] = OffsetDateTime.ofInstant(jInstant.ofEpochSecond(1), ZoneOffset.UTC)
                }
            }
        }
    }
}