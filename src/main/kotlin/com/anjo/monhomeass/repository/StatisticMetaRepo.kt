package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.model.dto.toStatisticMetaDto
import com.anjo.monhomeass.model.tables.StatisticMetaData
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StatisticMetaRepo(private val cache: RedisCache) {

    fun getAll(search: String? = null): List<StatisticMetaDto> {
        return cache.getOrPut("statistics-meta:${search ?: ""}") {
            transaction {
                var query = StatisticMetaData.selectAll()
                if (!search.isNullOrBlank()) {
                    val pattern = "%${search.lowercase()}%"
                    query = query.where {
                        (StatisticMetaData.statisticId.lowerCase() like pattern) or
                                (StatisticMetaData.name.lowerCase() like pattern)
                    }
                }
                query.map { it.toStatisticMetaDto() }
            }
        } ?: emptyList()
    }
}
