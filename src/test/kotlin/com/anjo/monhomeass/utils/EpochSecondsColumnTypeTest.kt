package com.anjo.monhomeass.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant

class EpochSecondsColumnTypeTest :
    StringSpec({

        "given class instance when check return type then assert response" {
            val epochSecondsColumnType = EpochSecondsColumnType()
            val expected = "DOUBLE PRECISION"

            val actual = epochSecondsColumnType.sqlType()

            actual shouldBe expected
            actual.shouldBeInstanceOf<String>()
        }

        "given double value when transform value then return new instant" {
            val doubleNumer = 1713132010.2021418
            val epochSecondsColumnType = EpochSecondsColumnType()
            val expected = Instant.fromEpochSeconds(1713132010, 202141761)
            val actual = epochSecondsColumnType.valueFromDB(doubleNumer)

            actual shouldBe expected
        }

        "given different type when transform value then return null" {
            val epochSecondsColumnType = EpochSecondsColumnType()
            val differentType = 123

            val actual = epochSecondsColumnType.valueFromDB(differentType)

            actual.shouldBeNull()
        }
    })
