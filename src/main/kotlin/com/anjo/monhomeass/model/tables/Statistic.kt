package com.anjo.monhomeass.model.tables

import com.anjo.monhomeass.utils.epochSeconds
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

// HA defines both statistics tables from one StatisticsBase mixin; same here.
abstract class StatisticTable(
    name: String,
) : Table(name) {
    val id = long("id")
    val created = timestampWithTimeZone("created").nullable()
    val createdTs = epochSeconds("created_ts").nullable()
    val metadataId = long("metadata_id").references(StatisticMetaData.id).nullable()
    val start = timestampWithTimeZone("start").nullable()
    val startTs = epochSeconds("start_ts").nullable()
    val mean = double("mean").nullable()
    val meanWeight = double("mean_weight").nullable()
    val min = double("min").nullable()
    val max = double("max").nullable()
    val lastReset = timestampWithTimeZone("last_reset").nullable()
    val lastResetTs = epochSeconds("last_reset_ts").nullable()
    val state = double("state").nullable()
    val sum = double("sum").nullable()
}

object Statistic : StatisticTable("statistics")

object StatisticShortTerm : StatisticTable("statistics_short_term")

object StatisticMetaData : Table("statistics_meta") {
    val id = long("id")
    val statisticId = varchar("statistic_id", 255).nullable()
    val sourceOfData = varchar("source", 32).nullable()
    val unitOfMeasurement = varchar("unit_of_measurement", 255).nullable()
    val unitClass = varchar("unit_class", 255).nullable()
    val hasMean = bool("has_mean").nullable()
    val hasSum = bool("has_sum").nullable()
    val name = varchar("name", 255).nullable()
    val meanType = short("mean_type")
    override val primaryKey = PrimaryKey(id)
}

object StatisticRun : Table("statistics_runs") {
    val id = long("run_id")
    val startedAt = timestampWithTimeZone("start")
}
