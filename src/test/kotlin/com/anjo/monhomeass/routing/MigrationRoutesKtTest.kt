package com.anjo.monhomeass.routing

import com.anjo.monhomeass.model.dto.MigrationDto
import com.anjo.monhomeass.model.tables.MigrationChanges
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
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test

class MigrationRoutesKtTest {

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
        val expected = listOf(MigrationDto("new_id", 1))

        //when
        val response = client.get("/migrations") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
        }
        val responseBody = response.bodyAsText()
        val decodedJson = Json.decodeFromString<List<MigrationDto>>(responseBody)

        //then
        response.status shouldBe HttpStatusCode.OK
        decodedJson shouldBe expected
    }

    private fun setupWithInsert() {
        prepareDatabase()
        transaction {
            SchemaUtils.create(MigrationChanges)
            addRecords {
                MigrationChanges.insert {
                    it[id] = "new_id"
                    it[version] = 1
                }
            }
        }
    }
}