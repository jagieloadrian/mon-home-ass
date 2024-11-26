package com.anjo.monhomeass.model.tables

import com.anjo.monhomeass.utils.epochSeconds
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object EventData : Table("event_data") {
    val id = integer("data_id")
    val hash = long("hash").nullable()
    val sharedData = text("shared_data").nullable()

    override val primaryKey = PrimaryKey(id)
}

object EventType : Table("event_types") {
    val id = integer("event_type_id")
    val type = varchar("event_type", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}

object Event : Table("events") {
    val id = integer("event_id")
    val type = char("event_type", 1).nullable()
    val data = char("event_data", 1).nullable()
    val origin = char("origin", 1).nullable()
    val originIdx = integer("origin_idx").nullable()
    val timestamp = timestampWithTimeZone("time_fired").nullable()
    val timestampTs = epochSeconds("time_fired_ts").nullable()
    val contextId = char("context_id", 1).nullable()
    val contextUserId = char("context_user_id", 1).nullable()
    val contextParentId = char("context_parent_id", 1).nullable()
    val dataId = integer("data_id").references(EventData.id).nullable()
    val contextIdBinary = binary("context_id_bin").nullable()
    val contextUserIdBinary = binary("context_user_id_bin").nullable()
    val contextParentIdBinary = binary("context_parent_id_bin").nullable()
    val eventTypeId = integer("event_type_id").references(EventType.id).nullable()
}