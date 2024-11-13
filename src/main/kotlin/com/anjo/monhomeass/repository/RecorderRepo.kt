package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.RecorderDto
import com.anjo.monhomeass.model.dto.toRecorderDto
import com.anjo.monhomeass.model.tables.Recorder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class RecorderRepo {

    fun getAll(): List<RecorderDto> {
        return transaction {
            Recorder.selectAll().map { it.toRecorderDto() }
        }
    }
}