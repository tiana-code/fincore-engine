// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.compliance.infrastructure.persistence.KycSessionPersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.KycSessionRepository
import com.fincore.core.KycSessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class KycServiceImpl(
    private val repository: KycSessionRepository,
    private val adapter: KycSessionPersistenceAdapter,
    private val idempotencyStore: KycIdempotencyStore,
) : KycService {
    override fun initiate(command: InitiateKycSessionCommand): KycSession {
        val keyHash = Sha256.hex(command.idempotencyKey)
        var attempt = 0
        while (true) {
            try {
                return idempotencyStore.reserveOrRun(keyHash) { createSession(command) }
            } catch (race: KycIdempotencyRaceException) {
                attempt++
                if (attempt >= MAX_ATTEMPTS) throw KycConcurrencyException(race)
            }
        }
    }

    private fun createSession(command: InitiateKycSessionCommand): KycSession {
        val session = KycSession(KycSessionId.generate(), command.subjectReference)
        repository.saveAndFlush(adapter.toNewEntity(session, Instant.now()))
        return session
    }

    @Transactional(readOnly = true)
    override fun get(id: KycSessionId): KycSession =
        adapter.toDomain(repository.findById(id.value).orElseThrow { KycSessionNotFoundException(id) })

    @Transactional
    override fun beginScreening(id: KycSessionId): KycSession = transition(id, KycStatus.SCREENING)

    @Transactional
    override fun approve(id: KycSessionId): KycSession = transition(id, KycStatus.APPROVED)

    @Transactional
    override fun reject(id: KycSessionId): KycSession = transition(id, KycStatus.REJECTED)

    private fun transition(
        id: KycSessionId,
        target: KycStatus,
    ): KycSession {
        val entity = repository.findById(id.value).orElseThrow { KycSessionNotFoundException(id) }
        val session = adapter.toDomain(entity)
        session.transitionTo(target)
        entity.status = session.status
        repository.saveAndFlush(entity)
        return session
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
