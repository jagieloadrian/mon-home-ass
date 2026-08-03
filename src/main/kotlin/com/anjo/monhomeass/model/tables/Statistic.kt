package com.anjo.monhomeass.model.tables

import com.anjo.monhomeass.utils.epochSeconds
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object Statistic : Table("statistics") {
    val id = integer("id")
    val created = timestampWithTimeZone("created").nullable()
    val createdTs = epochSeconds("created_ts").nullable()
    val metadataId = integer("metadata_id").references(StatisticMetaData.id).nullable()
    val start = timestampWithTimeZone("start").nullable()
    val startTs = epochSeconds("start_ts").nullable()
    val mean = double("mean").nullable()
    val min = double("min").nullable()
    val max = double("max").nullable()
    val lastReset = timestampWithTimeZone("last_reset").nullable()
    val lastResetTs = epochSeconds("last_reset_ts").nullable()
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
    override val primaryKey = PrimaryKey(id)
}

object StatisticRun : Table("statistics_runs") {
    val id = integer("run_id")
    val startedAt = timestampWithTimeZone("start")
}

object StatisticShortTem : Table("statistics_short_term") {
    val id = integer("id")
    val created = timestampWithTimeZone("created").nullable()
    val createdTs = epochSeconds("created_ts").nullable()
    val metadataId = integer("metadata_id").references(StatisticMetaData.id).nullable()
    val start = timestampWithTimeZone("start").nullable()
    val startTs = epochSeconds("start_ts").nullable()
    val mean = double("mean").nullable()
    val min = double("min").nullable()
    val max = double("max").nullable()
    val lastReset = timestampWithTimeZone("last_reset").nullable()
    val lastResetTs = epochSeconds("last_reset_ts").nullable()
    val state = double("state").nullable()
    val sum = double("sum").nullable()
}