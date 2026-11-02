package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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

        "given app running when get health probes and openapi then probes answer and API paths are documented" {
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
                val live = client.get("/health/live")
                val ready = client.get("/health/ready")
                val openApi = client.get("/openapi.json").bodyAsText()

                // then
                live.status shouldBe HttpStatusCode.OK
                ready.status shouldBe HttpStatusCode.OK
                openApi shouldContain "/statistics-meta"
                openApi shouldNotContain "/health"
            }
        }
    })
