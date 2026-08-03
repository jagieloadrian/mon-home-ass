package com.anjo.monhomeass.utils

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import kotlin.time.Instant

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

    override fun valueToDB(value: Instant?): Double? {
      return when(value) {
          is Instant -> value.epochSeconds + value.nanosecondsOfSecond / 1_000_000_000.0
          else -> null
      }
    }
}

fun Table.epochSeconds(name: String) = registerColumn(name, EpochSecondsColumnType())