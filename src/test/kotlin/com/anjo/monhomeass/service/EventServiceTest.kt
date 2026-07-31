package com.anjo.monhomeass.service

import com.anjo.monhomeass.model.dto.EventDto
import com.anjo.monhomeass.repository.EventRepo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class EventServiceTest : StringSpec({

    "given mock repo and null args when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()

        every { mockRepo.getAll() } returns expected

        //when
        val actual = service.getAll(null, null)

        //then
        actual shouldBe expected
        verify { mockRepo.getAll() }
    }

    "given mock repo and contain arg and exclude null when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val containArg = "someArg"

        every { mockRepo.getAllWithContain(containArg) } returns expected

        //when
        val actual = service.getAll(containArg, null)

        //then
        actual shouldBe expected
        verify { mockRepo.getAllWithContain(containArg) }
    }

    "given mock repo and exclude arg and contain null when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val excludeArg = "someArg"

        every { mockRepo.getAllWithExclude(excludeArg) } returns expected

        //when
        val actual = service.getAll(null, excludeArg)

        //then
        actual shouldBe expected
        verify { mockRepo.getAllWithExclude(excludeArg) }
    }

    "given mock repo and non null args when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val containArg = "someArg"
        val excludeArg = "someArg"

        every { mockRepo.getAllWithContainAndExclude(containArg, excludeArg) } returns expected

        //when
        val actual = service.getAll(containArg, excludeArg)

        //then
        actual shouldBe expected
        verify { mockRepo.getAllWithContainAndExclude(containArg, excludeArg) }
    }
})
