package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.dto.StatisticRunDto
import com.anjo.monhomeass.model.dto.toStatisticRunDto
import com.anjo.monhomeass.model.tables.StatisticRun
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StatisticRunRepo(
    private val cache: RedisCache,
) {
    fun getAll(limit: Int? = null): List<StatisticRunDto> =
        cache.getOrPut("statistics-runs:all:$limit") {
            transaction {
                var query =
                    StatisticRun
                        .selectAll()
                        .orderBy(StatisticRun.startedAt, SortOrder.DESC)
                if (limit != null) query = query.limit(limit)
                query.map { it.toStatisticRunDto() }
            }
        } ?: emptyList()
}
