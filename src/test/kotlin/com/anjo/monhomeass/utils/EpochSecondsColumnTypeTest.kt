package com.anjo.monhomeass.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant

class EpochSecondsColumnTypeTest :
    StringSpec({

        "given class instance when check return type then assert response" {
            // given
            val epochSecondsColumnType = EpochSecondsColumnType()
            val expected = "DOUBLE PRECISION"

            // when
            val actual = epochSecondsColumnType.sqlType()

            // then
            actual shouldBe expected
            actual.shouldBeInstanceOf<String>()
        }

        "given double value when transform value then return new instant" {
            // given
            val doubleNumer = 1713132010.2021418
            val epochSecondsColumnType = EpochSecondsColumnType()
            val expected = Instant.fromEpochSeconds(1713132010, 202141761)
            // when
            val actual = epochSecondsColumnType.valueFromDB(doubleNumer)

            // then
            actual shouldBe expected
        }

        "given different type when transform value then return null" {
            // given
            val epochSecondsColumnType = EpochSecondsColumnType()
            val differentType = 123

            // when
            val actual = epochSecondsColumnType.valueFromDB(differentType)

            // then
            actual.shouldBeNull()
        }
    })
