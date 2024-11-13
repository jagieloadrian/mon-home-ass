package com.anjo.monhomeass.model.tables

import org.jetbrains.exposed.sql.Table

object MigrationChanges : Table("migration_changes") {
    val id = varchar("migration_id", 255)
    val version = integer("version")
}