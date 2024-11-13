package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object Statistic : Table("statistics") {
    val id = integer("id")
    val created = timestampWithTimeZone("created").nullable()
    val createdTs = timestamp("created_ts").nullable()
    val metadataId = integer("metadata_id").references(StateMetadata.id).nullable()
    val start = timestampWithTimeZone("start").nullable()
    val startTs = timestamp("start_ts").nullable()
    val mean = double("mean").nullable()
    val min = double("min").nullable()
    val max = double("max").nullable()
    val lastReset = timestampWithTimeZone("last_reset").nullable()
    val lastResetTs = timestamp("last_reset_ts").nullable()
    val state = double("state").nullable()
    val sum = double("sum").nullable()
}

object StatisticMetaData : Table("statistics_meta") {
    val id = integer("data_id")
    val statisticId = varchar("statistic_id", 255).nullable()
    val sourceOfData = varchar("source", 32).nullable()
    val unitOfMeasurement = varchar("unit_of_measurement", 255).nullable()
    val hasMean = bool("has_mean").nullable()
    val hasSum = bool("has_sum").nullable()
    val name = varchar("name", 255).nullable()
}

object StatisticRun : Table("statistics_runs") {
    val id = integer("run_id")
    val startedAt = timestampWithTimeZone("start")
}

object StatisticShortTem : Table() {
    val id = integer("id")
    val created = timestampWithTimeZone("created").nullable()
    val createdTs = timestamp("created_ts").nullable()
    val metadataId = integer("metadata_id").references(StateMetadata.id).nullable()
    val start = timestampWithTimeZone("start").nullable()
    val startTs = timestamp("start_ts").nullable()
    val mean = double("mean").nullable()
    val min = double("min").nullable()
    val max = double("max").nullable()
    val lastReset = timestampWithTimeZone("last_reset").nullable()
    val lastResetTs = timestamp("last_reset_ts").nullable()
    val state = double("state").nullable()
    val sum = double("sum").nullable()
}