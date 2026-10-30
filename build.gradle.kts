import io.ktor.plugin.features.DockerPortMapping
import io.ktor.plugin.features.DockerPortMappingProtocol

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.ktlint)
    jacoco
}

group = "com.anjo"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.swagger)

    implementation(libs.postgresql)
    implementation(libs.mariadb)
    implementation(libs.mysql)
    implementation(libs.sqlite)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    implementation(libs.khealth)
    implementation(libs.jedis)
    implementation(libs.jnats)
    implementation(libs.kotlin.logging)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
}

ktlint {
    version =
        libs.versions.ktlint.engine
            .get()
}

tasks.test {
    useJUnitPlatform()
    filter { excludeTestsMatching("com.anjo.monhomeass.tools.OpenApiSnapshotKtTest") }
    finalizedBy(tasks.jacocoTestReport)
}

// ponytail: reuses the test source set (already has testApplication +
// testcontainers wired) instead of a standalone main()/source set just to
// boot the app and hit one endpoint.
tasks.register<Test>("generateOpenApiSpec") {
    description = "Snapshots the live-generated OpenAPI doc to openapi.json"
    group = "documentation"
    useJUnitPlatform()
    val testSources = sourceSets.test.get()
    testClassesDirs = testSources.output.classesDirs
    classpath = testSources.runtimeClasspath
    filter { includeTestsMatching("com.anjo.monhomeass.tools.OpenApiSnapshotKtTest") }
    outputs.upToDateWhen { false }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_21)
        customBaseImage.set("eclipse-temurin:21-jre")
        localImageName.set(rootProject.name)
        imageTag.set(version.toString())
        portMappings.set(
            listOf(
                DockerPortMapping(outsideDocker = 8080, 8080, DockerPortMappingProtocol.TCP),
            ),
        )
    }
}

// Homelab nodes: RPi5 (arm64) + Wyse (amd64). User matches the pod securityContext.
jib {
    from {
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    container {
        user = "10001:10001"
        jvmFlags = listOf("-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError")
        creationTime.set("USE_CURRENT_TIMESTAMP")
    }
}
