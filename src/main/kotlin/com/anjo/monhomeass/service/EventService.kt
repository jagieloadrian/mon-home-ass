package com.anjo.monhomeass.service

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.repository.EventRepo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Objects.isNull

class EventService(private val repository: EventRepo = EventRepo()) {
    private val logger = KotlinLogging.logger {}

    fun getAll(contain: String?, exclude: String?): List<EventDto> {
        return when {
            isNull(contain) && isNull(exclude) -> {
                logger.info { "Using default method: getAll()" }
                repository.getAll()
            }
            isNull(contain) && exclude != null -> {
                logger.info { "Using method: getAllWithExclude()" }
                repository.getAllWithExclude(exclude)
            }
            contain != null && isNull(exclude) -> {
                logger.info { "Using method: getAllWithContain()" }
                repository.getAllWithContain(contain)
            }
            contain != null && exclude != null -> {
                logger.info { "Using method: getAllWithContainAndExclude()" }

                repository.getAllWithContainAndExclude(contain, exclude)
            }
            else                               -> {
                logger.info { "There's some strange things happened. Please, debug what was that" }
                throw IllegalArgumentException("Inner exception")
            }
        }
    }
}