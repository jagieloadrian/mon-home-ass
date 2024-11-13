package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

object SchemaChanges : Table("schema_changes") {
    val id = integer("change_id")
    val version = integer("schema_version").nullable()
    val changedAt = timestampWithTimeZone("changed")
}