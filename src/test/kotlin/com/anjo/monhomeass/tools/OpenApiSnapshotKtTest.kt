package com.anjo.monhomeass.tools

import com.anjo.monhomeass.plugins.configureRouting
import com.anjo.monhomeass.testutils.prepareDatabase
import com.anjo.monhomeass.testutils.testApplicationConfig
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import java.io.File

// ponytail: not a second source of truth — dumps the app's own live-generated
// OpenAPI doc (served at /openapi.json and /openapi.yaml) to disk for tools
// that need a static file (codegen, external doc hosting). Run explicitly via
// `./gradlew generateOpenApiSpec`; excluded from the normal `test` task.
class OpenApiSnapshotKtTest : StringSpec({
    "write openapi snapshots to disk" {
        testApplication {
            prepareDatabase()
            environment {
                config = testApplicationConfig()
            }
            application {
                configureRouting()
            }

            File("openapi.json").writeText(client.get("/openapi.json").bodyAsText())
            File("openapi.yaml").writeText(client.get("/openapi.yaml").bodyAsText())
        }
    }
})
