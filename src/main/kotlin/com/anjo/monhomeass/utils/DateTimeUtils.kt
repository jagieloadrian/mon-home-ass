package com.anjo.monhomeass.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.OffsetDateTime

fun OffsetDateTime.toKotlinxDateTime(): LocalDateTime {
    return this.toLocalDateTime().toKotlinLocalDateTime()
}