package com.anjo.monhomeass.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.LocalDateTime
import java.time.OffsetDateTime

class DateTimeUtilsKtTest :
    StringSpec({

        "given a offsetdatetime when convert to kotlindatetime then return properly type of object" {
            // given
            val date = OffsetDateTime.now()

            // when
            val actual = date.toKotlinxDateTime()

            // then
            actual.shouldBeInstanceOf<LocalDateTime>()
        }
    })
