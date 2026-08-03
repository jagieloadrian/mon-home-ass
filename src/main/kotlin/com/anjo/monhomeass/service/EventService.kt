package com.anjo.monhomeass.service

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.repository.EventRepo

class EventService(private val repository: EventRepo = EventRepo()) {

    fun getAll(contain: String?, exclude: String?): List<EventDto> {
        return repository.getAll(contain, exclude)
    }
}
