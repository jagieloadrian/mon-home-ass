package com.anjo.monhomeass.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class DatabasesKtTest : StringSpec({

    "given postgres type when generateUrl then returns postgres jdbc url" {
        generateUrl("postgres", "host:5432", "home_assistant") shouldBe
                "jdbc:postgresql://host:5432/home_assistant"
    }

    "given mariadb type when generateUrl then returns mariadb jdbc url" {
        generateUrl("mariadb", "host:3306", "home_assistant") shouldBe
                "jdbc:mariadb://host:3306/home_assistant"
    }

    "given mysql type when generateUrl then returns mysql jdbc url" {
        generateUrl("mysql", "host:3306", "home_assistant") shouldBe
                "jdbc:mysql://host:3306/home_assistant"
    }

    "given sqlite type when generateUrl then returns readonly sqlite jdbc url" {
        generateUrl("sqlite", "", "", "/data/home-assistant_v2.db") shouldBe
                "jdbc:sqlite:file:/data/home-assistant_v2.db?mode=ro"
    }

    "given unsupported type when generateUrl then throws naming the value and valid options" {
        val exception = shouldThrow<IllegalStateException> {
            generateUrl("mongodb", "host", "db")
        }
        exception.message shouldContain "mongodb"
        exception.message shouldContain "postgres"
        exception.message shouldContain "mariadb"
        exception.message shouldContain "mysql"
        exception.message shouldContain "sqlite"
    }
})
