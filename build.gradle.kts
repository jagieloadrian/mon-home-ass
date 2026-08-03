import io.ktor.plugin.features.*

val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val exposed_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    jacoco
}

group = "com.anjo"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    //ktor
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-default-headers-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-di")

    //swagger
    implementation("io.ktor:ktor-server-swagger:${ktor_version}")
    //error handling
    implementation("io.ktor:ktor-server-status-pages-jvm")

    //databases
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.6")
    implementation("com.mysql:mysql-connector-j:9.5.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    //health
    implementation("dev.hayden:khealth:3.0.0")
    //cache
    implementation("redis.clients:jedis:7.5.3")
    //logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    //test
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:mariadb:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
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
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
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
        customBaseImage.set("eclipse-temurin:21-jre-alpine")
        localImageName.set(rootProject.name)
        imageTag.set(version.toString())
        jib.container.creationTime = "USE_CURRENT_TIMESTAMP"
        portMappings.set(listOf(
                DockerPortMapping(outsideDocker = 8080, 8080, DockerPortMappingProtocol.TCP)
        )
        )
    }
}
