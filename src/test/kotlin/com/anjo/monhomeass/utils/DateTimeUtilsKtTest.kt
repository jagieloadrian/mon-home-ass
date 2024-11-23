package com.anjo.monhomeass.utils

import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.LocalDateTime
import java.time.OffsetDateTime
import kotlin.test.Test

class DateTimeUtilsKtTest {

    @Test
    fun `given a offsetdatetime when convert to kotlindatetime then return properly type of object`() {
        //given
        val date = OffsetDateTime.now()

        //when
        val actual = date.toKotlinxDateTime()

        //then
        actual.shouldBeInstanceOf<LocalDateTime>()
    }
}
