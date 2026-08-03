package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.dto.StateDto
import com.anjo.monhomeass.model.dto.toStateDto
import com.anjo.monhomeass.model.tables.State
import com.anjo.monhomeass.model.tables.StateAttributes
import com.anjo.monhomeass.model.tables.StateMetadata
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class StateRepo(private val cache: RedisCache) {

    private fun stateJoin(): Join {
        return Join(
                State,
                StateMetadata,
                JoinType.INNER,
                additionalConstraint = { State.metadataId eq StateMetadata.id }
        ).join(
                StateAttributes,
                JoinType.LEFT,
                additionalConstraint = { State.attributesId eq StateAttributes.id }
        )
    }

    // ponytail: unknown entityId and known-entity-with-zero-rows both resolve to
    // null here on purpose — both mean 404 on the latest-state route (FR-003).
    fun getLatest(entityId: String): StateDto? {
        return cache.getOrPut("states:latest:$entityId") {
            transaction {
                stateJoin().selectAll()
                        .where { StateMetadata.entityId eq entityId }
                        .orderBy(State.lastUpdatedTs, SortOrder.DESC)
                        .limit(1)
                        .map { it.toStateDto() }
                        .firstOrNull()
            }
        }
    }

    fun getHistory(entityId: String, from: Instant?, to: Instant?, limit: Int? = null, offset: Long? = null): List<StateDto>? {
        return cache.getOrPut("states:history:$entityId:$from:$to:$limit:$offset") {
            transaction {
                val exists = StateMetadata
                        .select(StateMetadata.id)
                        .where { StateMetadata.entityId eq entityId }
                        .limit(1)
                        .any()
                if (!exists) return@transaction null

                var query = stateJoin().selectAll()
                        .where { StateMetadata.entityId eq entityId }
                        .andWhere { State.lastUpdatedTs.isNotNull() }
                if (from != null) query = query.andWhere { State.lastUpdatedTs greaterEq from }
                if (to != null) query = query.andWhere { State.lastUpdatedTs lessEq to }

                query = query.orderBy(State.lastUpdatedTs, SortOrder.ASC)
                if (limit != null) query = query.limit(limit)
                if (offset != null) query = query.offset(offset)

                query.map { it.toStateDto() }
            }
        }
    }
}
