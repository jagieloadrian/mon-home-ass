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

        every { mockRepo.getAll(null, null) } returns expected

        //when
        val actual = service.getAll(null, null)

        //then
        actual shouldBe expected
        verify { mockRepo.getAll(null, null) }
    }

    "given mock repo and contain arg and exclude null when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val containArg = "someArg"

        every { mockRepo.getAll(containArg, null) } returns expected

        //when
        val actual = service.getAll(containArg, null)

        //then
        actual shouldBe expected
        verify { mockRepo.getAll(containArg, null) }
    }

    "given mock repo and exclude arg and contain null when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val excludeArg = "someArg"

        every { mockRepo.getAll(null, excludeArg) } returns expected

        //when
        val actual = service.getAll(null, excludeArg)

        //then
        actual shouldBe expected
        verify { mockRepo.getAll(null, excludeArg) }
    }

    "given mock repo and non null args when get all then verify using of specific method" {
        //given
        val mockRepo = mockk<EventRepo>()
        val service = EventService(mockRepo)
        val expected = listOf<EventDto>()
        val containArg = "someArg"
        val excludeArg = "someArg"

        every { mockRepo.getAll(containArg, excludeArg) } returns expected

        //when
        val actual = service.getAll(containArg, excludeArg)

        //then
        actual shouldBe expected
        verify { mockRepo.getAll(containArg, excludeArg) }
    }
})
