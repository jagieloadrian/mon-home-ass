package com.anjo.monhomeass.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication

private suspend fun HttpClient.preflight(origin: String): HttpResponse =
    options("/statistics-meta") {
        header(HttpHeaders.Origin, origin)
        header(HttpHeaders.AccessControlRequestMethod, "GET")
    }

class HTTPKtTest :
    StringSpec({

        "given CORS hosts configured when preflight then only a listed origin is allowed" {
            testApplication {
                // given
                environment {
                    config = MapApplicationConfig("ktor.cors.hosts" to " statistics.home.arpa, ,localhost:8081")
                }
                application {
                    configureHTTP()
                }

                // when
                val listed = client.preflight("https://statistics.home.arpa")
                val local = client.preflight("http://localhost:8081")
                val other = client.preflight("https://evil.example.com")

                // then
                listed.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe "https://statistics.home.arpa"
                local.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe "http://localhost:8081"
                other.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe null
            }
        }

        "given empty CORS hosts when preflight then no origin is allowed" {
            testApplication {
                // given
                environment {
                    config = MapApplicationConfig("ktor.cors.hosts" to "")
                }
                application {
                    configureHTTP()
                }

                // when
                val response = client.preflight("https://statistics.home.arpa")

                // then
                response.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe null
            }
        }
    })
