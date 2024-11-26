package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.SchemaChangeDto
import com.anjo.monhomeass.model.tables.SchemaChanges
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test

class SchemaChangesRoutesKtTest {

    @Test
    fun `given migration routes when get the return migration dto`() = testApplication {
        //given
        setupWithInsert()
        environment {
            config = ApplicationConfig("application-test.yaml")
        }
        application {
            configureDatabases()
            configureRouting()
        }
        val date = Instant.fromEpochSeconds(1).toLocalDateTime(TimeZone.UTC)
        val expected = listOf(SchemaChangeDto(1, 1, date))

        //when
        val response = client.get("/schemas") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val responseBody = response.bodyAsText()
        val decodedJson = Json.decodeFromString<List<SchemaChangeDto>>(responseBody)

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    private fun setupWithInsert() {
        prepareDatabase()
        transaction {
            SchemaUtils.create(SchemaChanges)
            addRecords {
                SchemaChanges.insert {
                    it[id] = 1
                    it[version] = 1
                    it[changedAt] = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1), ZoneOffset.UTC)
                }
            }
        }
    }
}