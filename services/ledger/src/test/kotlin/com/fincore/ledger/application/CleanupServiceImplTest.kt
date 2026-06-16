// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fincore.events.OutboxStatus
import com.fincore.ledger.config.CleanupProperties
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration

class CleanupServiceImplTest {
    private val idempotencyKeyRepository = mockk<IdempotencyKeyRepository>()
    private val outboxEventRepository = mockk<OutboxEventRepository>()
    private val service =
        CleanupServiceImpl(
            idempotencyKeyRepository,
            outboxEventRepository,
            CleanupProperties(outboxRetention = Duration.ofDays(30)),
        )

    @Test
    fun `should delegate to both repositories and return the deleted counts`() {
        every { idempotencyKeyRepository.deleteExpiredBefore(any()) } returns 3
        every { outboxEventRepository.deletePublishedBefore(OutboxStatus.PUBLISHED, any()) } returns 5

        val result = service.purge()

        result shouldBe CleanupResult(3, 5)
        verify { idempotencyKeyRepository.deleteExpiredBefore(any()) }
        verify { outboxEventRepository.deletePublishedBefore(OutboxStatus.PUBLISHED, any()) }
    }

    @Test
    fun `should log the deleted counts as structured fields`() {
        every { idempotencyKeyRepository.deleteExpiredBefore(any()) } returns 2
        every { outboxEventRepository.deletePublishedBefore(OutboxStatus.PUBLISHED, any()) } returns 4
        val appender = attachAppender()

        service.purge()

        val event = appender.list.single()
        event.keyValuePairs.associate { it.key to it.value } shouldBe
            mapOf("idempotencyKeysDeleted" to 2, "outboxEventsDeleted" to 4)
    }

    private fun attachAppender(): ListAppender<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(CleanupServiceImpl::class.java) as Logger
        return ListAppender<ILoggingEvent>().apply {
            start()
            logger.addAppender(this)
        }
    }
}
