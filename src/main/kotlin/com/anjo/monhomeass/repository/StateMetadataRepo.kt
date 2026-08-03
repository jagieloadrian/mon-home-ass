package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.dto.EntityDto
import com.anjo.monhomeass.model.dto.toEntityDto
import com.anjo.monhomeass.model.tables.StateMetadata
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StateMetadataRepo(private val cache: RedisCache) {

    fun getAll(): List<EntityDto> {
        return cache.getOrPut("entities:all") {
            transaction {
                StateMetadata.selectAll().map { it.toEntityDto() }
            }
        } ?: emptyList()
    }
}
