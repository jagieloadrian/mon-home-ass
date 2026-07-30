package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.StatisticMetaDto
import com.anjo.monhomeass.model.dto.toStatisticMetaDto
import com.anjo.monhomeass.model.tables.StatisticMetaData
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StatisticMetaRepo {

    fun getAll(): List<StatisticMetaDto> {
        return transaction {
            StatisticMetaData.selectAll().map { it.toStatisticMetaDto() }
        }
    }
}
