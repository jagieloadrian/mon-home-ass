package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.StatisticDto
import com.anjo.monhomeass.model.dto.StatisticSummaryDto
import com.anjo.monhomeass.model.dto.toStatisticDto
import com.anjo.monhomeass.model.dto.toStatisticDtoShortTerm
import com.anjo.monhomeass.model.tables.Statistic
import com.anjo.monhomeass.model.tables.StatisticMetaData
import com.anjo.monhomeass.model.tables.StatisticShortTem
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.eq
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

class StatisticRepo {

    fun getLongTerm(statisticId: String, from: Instant?, to: Instant?, limit: Int? = null, offset: Long? = null): List<StatisticDto>? {
        return transaction {
            val exists = StatisticMetaData
                    .select(StatisticMetaData.id)
                    .where { StatisticMetaData.statisticId eq statisticId }
                    .limit(1)
                    .any()
            if (!exists) return@transaction null

            val join = Join(
                    Statistic,
                    StatisticMetaData,
                    JoinType.INNER,
                    additionalConstraint = { Statistic.metadataId eq StatisticMetaData.id }
            )

            var query = join.selectAll()
                    .where { StatisticMetaData.statisticId eq statisticId }
                    .andWhere { Statistic.startTs.isNotNull() }
            if (from != null) query = query.andWhere { Statistic.startTs greaterEq from }
            if (to != null) query = query.andWhere { Statistic.startTs lessEq to }

            query = query.orderBy(Statistic.startTs, SortOrder.ASC)
            if (limit != null) query = query.limit(limit)
            if (offset != null) query = query.offset(offset)

            query.map { it.toStatisticDto() }
        }
    }

    fun getShortTerm(statisticId: String, from: Instant?, to: Instant?, limit: Int? = null, offset: Long? = null): List<StatisticDto>? {
        return transaction {
            val exists = StatisticMetaData
                    .select(StatisticMetaData.id)
                    .where { StatisticMetaData.statisticId eq statisticId }
                    .limit(1)
                    .any()
            if (!exists) return@transaction null

            val join = Join(
                    StatisticShortTem,
                    StatisticMetaData,
                    JoinType.INNER,
                    additionalConstraint = { StatisticShortTem.metadataId eq StatisticMetaData.id }
            )

            var query = join.selectAll()
                    .where { StatisticMetaData.statisticId eq statisticId }
                    .andWhere { StatisticShortTem.startTs.isNotNull() }
            if (from != null) query = query.andWhere { StatisticShortTem.startTs greaterEq from }
            if (to != null) query = query.andWhere { StatisticShortTem.startTs lessEq to }

            query = query.orderBy(StatisticShortTem.startTs, SortOrder.ASC)
            if (limit != null) query = query.limit(limit)
            if (offset != null) query = query.offset(offset)

            query.map { it.toStatisticDtoShortTerm() }
        }
    }

    fun getSummary(statisticId: String, from: Instant?, to: Instant?): StatisticSummaryDto? {
        return transaction {
            val exists = StatisticMetaData
                    .select(StatisticMetaData.id)
                    .where { StatisticMetaData.statisticId eq statisticId }
                    .limit(1)
                    .any()
            if (!exists) return@transaction null

            val join = Join(
                    Statistic,
                    StatisticMetaData,
                    JoinType.INNER,
                    additionalConstraint = { Statistic.metadataId eq StatisticMetaData.id }
            )

            val minExpr = Statistic.state.min()
            val maxExpr = Statistic.state.max()
            val avgExpr = Statistic.state.avg()
            val sumExpr = Statistic.state.sum()

            var query = join.select(minExpr, maxExpr, avgExpr, sumExpr)
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
                    sum = row[sumExpr]
            )
        }
    }
}
