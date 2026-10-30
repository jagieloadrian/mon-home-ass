package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object SchemaChanges : Table("schema_changes") {
    val id = long("change_id")
    val version = integer("schema_version").nullable()
    val changedAt = timestampWithTimeZone("changed")
}
