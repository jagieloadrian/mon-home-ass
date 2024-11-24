package com.anjo.monhomeass.utils

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table

class EpochSecondsColumnType : ColumnType<Instant>() {
    override fun sqlType(): String {
        return "DOUBLE PRECISION"
    }

    override fun valueFromDB(value: Any): Instant? {
        return when(value) {
            is Double -> Instant.fromEpochSeconds(value.toLong(), ((value % 1) * 1_000_000_000).toLong())
            else -> null
        }
    }
}

fun Table.epochSeconds(name: String) = registerColumn(name, EpochSecondsColumnType())