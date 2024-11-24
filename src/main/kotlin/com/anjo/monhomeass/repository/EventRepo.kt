package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.model.dto.toEventDto
import com.anjo.monhomeass.model.tables.Event
import com.anjo.monhomeass.model.tables.EventData
import com.anjo.monhomeass.model.tables.EventType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class EventRepo {

    fun getAll(): List<EventDto> {
        return transaction {
            Event.innerJoin(EventData)
                    .innerJoin(EventType)
                    .selectAll()
                    .orderBy(Event.timestampTs, SortOrder.DESC)
                    .map { it.toEventDto() }
        }
    }

    fun getAllWithContain(contain: String): List<EventDto> {
        return transaction {
            Event.innerJoin(EventData)
                    .innerJoin(EventType)
                    .selectAll()
                    .where { EventData.sharedData.like("%$contain%") }
                    .orderBy(Event.timestampTs, SortOrder.DESC)
                    .map { it.toEventDto() }
        }
    }

    fun getAllWithExclude(exclude: String): List<EventDto> {
        return transaction {
            Event.innerJoin(EventData)
                    .innerJoin(EventType)
                    .selectAll()
                    .where { EventData.sharedData.notLike("%$exclude%") }
                    .orderBy(Event.timestampTs, SortOrder.DESC)
                    .map { it.toEventDto() }
        }
    }

    fun getAllWithContainAndExclude(contain: String, exclude: String): List<EventDto> {
        return transaction {
            Event.innerJoin(EventData)
                    .innerJoin(EventType)
                    .selectAll()
                    .where { EventData.sharedData.like("%$contain%") }
                    .andWhere { EventData.sharedData.notLike("%$exclude%") }
                    .orderBy(Event.timestampTs, SortOrder.DESC)
                    .map { it.toEventDto() }
        }
    }
}
