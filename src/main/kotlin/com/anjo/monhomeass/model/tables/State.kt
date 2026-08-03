package com.anjo.monhomeass.model.tables

import com.anjo.monhomeass.utils.epochSeconds
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object StateAttributes : Table() {
    val id = integer("attributes_id")
    val hash = long("hash").nullable()
    val sharedAttributes = text("shared_attrs").nullable()
    override val primaryKey = PrimaryKey(id)
}

object State : Table("states") {
    val id = integer("state_id")
    val entityId = char("entity_id", 1).nullable()
    val state = varchar("state", 255).nullable()
    val attributes = char("attributes", 1).nullable()
    val lastModified = timestampWithTimeZone("last_changed").nullable()
    val lastModifiedTs = epochSeconds("last_changed_ts").nullable()
    val lastReportedTs = epochSeconds("last_reported_ts").nullable()
    val lastUpdated = timestampWithTimeZone("last_updated").nullable()
    val oldId = integer("old_state_id").references(id).nullable()
    val attributesId = integer("attributes_id").references(StateAttributes.id).nullable()
    val lastUpdatedTs = epochSeconds("last_updated_ts").nullable()
    val contextId = char("context_id", 1).nullable()
    val contextUserId = char("context_user_id", 1).nullable()
    val contextParentId = char("context_parent_id", 1).nullable()
    val dataId = integer("origin_idx").nullable()
    val contextIdBinary = binary("context_id_bin").nullable()
    val contextUserIdBinary = binary("context_user_id_bin").nullable()
    val contextParentIdBinary = binary("context_parent_id_bin").nullable()
    val metadataId = integer("metadata_id").references(StateMetadata.id).nullable()
    override val primaryKey = PrimaryKey(id)
}

object StateMetadata : Table("states_meta") {
    val id = integer("metadata_id")
    val entityId = varchar("entity_id", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}