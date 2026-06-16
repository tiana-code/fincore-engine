// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.events.OutboxStatus
import com.fincore.ledger.config.CleanupProperties
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CleanupServiceImpl(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val properties: CleanupProperties,
) : CleanupService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun purge(): CleanupResult {
        val now = Instant.now()
        val idempotencyKeysDeleted = idempotencyKeyRepository.deleteExpiredBefore(now)
        val outboxEventsDeleted =
            outboxEventRepository.deletePublishedBefore(OutboxStatus.PUBLISHED, now.minus(properties.outboxRetention))
        log
            .atInfo()
            .addKeyValue("idempotencyKeysDeleted", idempotencyKeysDeleted)
            .addKeyValue("outboxEventsDeleted", outboxEventsDeleted)
            .log("ledger cleanup purge completed")
        return CleanupResult(idempotencyKeysDeleted, outboxEventsDeleted)
    }
}
