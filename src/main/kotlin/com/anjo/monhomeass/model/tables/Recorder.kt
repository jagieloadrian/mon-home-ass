package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Recorder : Table("recorder_runs") {
    val runId = long("run_id")
    val startTime = timestampWithTimeZone("start")
    val endTime = timestampWithTimeZone("end").nullable()
    val isClosedIncorrect = bool("closed_incorrect")
    val createdAt = timestampWithTimeZone("created")
}
