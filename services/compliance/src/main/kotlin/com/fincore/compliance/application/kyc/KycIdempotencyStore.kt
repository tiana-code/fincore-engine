// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.infrastructure.persistence.KycIdempotencyKeyEntity
import com.fincore.compliance.infrastructure.persistence.KycIdempotencyKeyRepository
import com.fincore.compliance.infrastructure.persistence.KycSessionPersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.KycSessionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// Raised when a concurrent caller already committed this idempotency key. The service retries in a fresh transaction,
// where the winner's committed key is found and its session replayed.
internal class KycIdempotencyRaceException(
    cause: Throwable,
) : RuntimeException(cause)

@Component
class KycIdempotencyStore(
    private val keyRepository: KycIdempotencyKeyRepository,
    private val sessionRepository: KycSessionRepository,
    private val adapter: KycSessionPersistenceAdapter,
) {
    @Transactional
    fun reserveOrRun(
        keyHash: String,
        action: () -> KycSession,
    ): KycSession {
        val existing = keyRepository.findById(keyHash).orElse(null)
        if (existing != null) {
            val entity =
                sessionRepository.findById(existing.kycSessionId).orElseThrow {
                    IllegalStateException("Idempotency key references a missing session ${existing.kycSessionId}")
                }
            return adapter.toDomain(entity)
        }
        val session = action()
        try {
            keyRepository.saveAndFlush(KycIdempotencyKeyEntity(keyHash, session.id.value, Instant.now()))
        } catch (duplicate: DataIntegrityViolationException) {
            throw KycIdempotencyRaceException(duplicate)
        }
        return session
    }
}
