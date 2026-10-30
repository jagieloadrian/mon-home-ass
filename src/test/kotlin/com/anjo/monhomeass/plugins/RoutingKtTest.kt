package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class RoutingKtTest :
    StringSpec({

        "given app running when get swagger then returns OpenAPI UI page" {
            testApplication {
                // given
                prepareDatabase()
                environment {
                    config = testApplicationConfig()
                }
                application {
                    configureDatabases()
                    configureStatusPages()
                    configureRouting()
                }

                // when
                val response = client.get("/swagger")

                // then
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "swagger"
            }
        }
    })
