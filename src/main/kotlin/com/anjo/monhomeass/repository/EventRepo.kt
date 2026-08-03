package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.model.dto.toEventDto
import com.anjo.monhomeass.model.tables.Event
import com.anjo.monhomeass.model.tables.EventData
import com.anjo.monhomeass.model.tables.EventType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.notLike
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EventRepo {

    fun getAll(contain: String? = null, exclude: String? = null): List<EventDto> {
        return transaction {
            var query = Event.innerJoin(EventData)
                    .innerJoin(EventType)
                    .selectAll()
            if (contain != null) query = query.andWhere { EventData.sharedData.like("%$contain%") }
            if (exclude != null) query = query.andWhere { EventData.sharedData.notLike("%$exclude%") }
            query.orderBy(Event.timestampTs, SortOrder.DESC)
                    .map { it.toEventDto() }
        }
    }
}
