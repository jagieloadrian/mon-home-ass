package com.anjo.monhomeass.repository

import com.anjo.monhomeass.cache.RedisCache
import com.anjo.monhomeass.model.dto.StatisticDto
import com.anjo.monhomeass.model.dto.StatisticRecordDto
import com.anjo.monhomeass.model.dto.StatisticSummaryDto
import com.anjo.monhomeass.model.dto.toStatisticDto
import com.anjo.monhomeass.model.dto.toStatisticRecordDto
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTerm
import com.anjo.monhomeass.model.tables.StatisticTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Instant

class StatisticRepo(
    private val cache: RedisCache,
) {
    fun getLongTerm(
        statisticId: String,
        from: Instant?,
        to: Instant?,
        limit: Int? = null,
        offset: Long? = null,
    ): List<StatisticDto>? = getRows(Statistic, "long-term", statisticId, from, to, limit, offset)

    fun getShortTerm(
        statisticId: String,
        from: Instant?,
        to: Instant?,
        limit: Int? = null,
        offset: Long? = null,
    ): List<StatisticDto>? = getRows(StatisticShortTerm, "short-term", statisticId, from, to, limit, offset)

    fun getSummary(
        statisticId: String,
        from: Instant?,
        to: Instant?,
    ): StatisticSummaryDto? =
        cache.getOrPut("statistics:summary:$statisticId:$from:$to") {
            transaction {
                if (!statisticExists(statisticId)) return@transaction null

                val minExpr = Statistic.state.min()
                val maxExpr = Statistic.state.max()
                val avgExpr = Statistic.state.avg()
                val sumExpr = Statistic.state.sum()

                var query =
                    Statistic
                        .innerJoin(StatisticMetaData)
                        .select(minExpr, maxExpr, avgExpr, sumExpr)
                        .where { StatisticMetaData.statisticId eq statisticId }
                if (from != null) query = query.andWhere { Statistic.startTs greaterEq from }
                if (to != null) query = query.andWhere { Statistic.startTs lessEq to }

                val row = query.first()
                StatisticSummaryDto(
                    statisticId = statisticId,
                    from = from?.toString(),
                    to = to?.toString(),
                    min = row[minExpr],
                    max = row[maxExpr],
                    avg = row[avgExpr]?.toDouble(),
                    sum = row[sumExpr],
                )
            }
        }

    fun getShortTermAfter(
        afterId: Long,
        limit: Int,
    ): List<Pair<Long, StatisticRecordDto>> =
        transaction {
            StatisticShortTerm
                .innerJoin(StatisticMetaData)
                .selectAll()
                .where { StatisticShortTerm.id greater afterId }
                .andWhere { StatisticShortTerm.startTs.isNotNull() }
                .andWhere { StatisticMetaData.statisticId.isNotNull() }
                .orderBy(StatisticShortTerm.id, SortOrder.ASC)
                .limit(limit)
                .mapNotNull { row -> row.toStatisticRecordDto()?.let { row[StatisticShortTerm.id] to it } }
        }

    private fun getRows(
        table: StatisticTable,
        cacheKind: String,
        statisticId: String,
        from: Instant?,
        to: Instant?,
        limit: Int?,
        offset: Long?,
    ): List<StatisticDto>? =
        cache.getOrPut("statistics:$cacheKind:$statisticId:$from:$to:$limit:$offset") {
            transaction {
                if (!statisticExists(statisticId)) return@transaction null

                var query =
                    table
                        .innerJoin(StatisticMetaData)
                        .selectAll()
                        .where { StatisticMetaData.statisticId eq statisticId }
                        .andWhere { table.startTs.isNotNull() }
                if (from != null) query = query.andWhere { table.startTs greaterEq from }
                if (to != null) query = query.andWhere { table.startTs lessEq to }

                query = query.orderBy(table.startTs, SortOrder.ASC)
                if (limit != null) query = query.limit(limit)
                if (offset != null) query = query.offset(offset)

                query.mapNotNull { it.toStatisticDto(table) }
            }
        }

    private fun statisticExists(statisticId: String): Boolean =
        StatisticMetaData
            .select(StatisticMetaData.id)
            .where { StatisticMetaData.statisticId eq statisticId }
            .limit(1)
            .any()
}
