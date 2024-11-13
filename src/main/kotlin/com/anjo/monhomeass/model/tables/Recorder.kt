package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object Recorder : Table("recorder_runs") {
    val runId = integer("run_id")
    val startTime = timestampWithTimeZone("start")
    val endTime = timestampWithTimeZone("end").nullable()
    val isClosedIncorrect = bool("closed_incorrect")
    val createdAt = timestampWithTimeZone("created")
}