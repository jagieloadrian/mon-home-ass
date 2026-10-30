package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.RecorderDto
import com.anjo.monhomeass.model.dto.toRecorderDto
import com.anjo.monhomeass.model.tables.Recorder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class RecorderRepo {
    fun getAll(): List<RecorderDto> =
        transaction {
            Recorder.selectAll().map { it.toRecorderDto() }
        }
}
