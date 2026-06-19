// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.ComplianceDomainException
import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.compliance.infrastructure.persistence.KycSessionEntity
import com.fincore.compliance.infrastructure.persistence.KycSessionPersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.KycSessionRepository
import com.fincore.core.KycSessionId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class KycServiceImplTest {
    private val repository = mockk<KycSessionRepository>()
    private val idempotencyStore = mockk<KycIdempotencyStore>()
    private val service = KycServiceImpl(repository, KycSessionPersistenceAdapter(), idempotencyStore)

    @Test
    fun `should create an initiated session when initiating`() {
        every { repository.saveAndFlush(any()) } answers { firstArg() }
        every { idempotencyStore.reserveOrRun(any(), any()) } answers { secondArg<() -> KycSession>().invoke() }

        val session = service.initiate(InitiateKycSessionCommand("key-1", "subject-1"))

        session.status shouldBe KycStatus.INITIATED
        session.subjectReference shouldBe "subject-1"
    }

    @Test
    fun `should throw a concurrency exception when the idempotency key keeps racing`() {
        every { idempotencyStore.reserveOrRun(any(), any()) } throws KycIdempotencyRaceException(RuntimeException("dup"))

        shouldThrow<KycConcurrencyException> { service.initiate(InitiateKycSessionCommand("key-1", "subject-1")) }
    }

    @Test
    fun `should throw not found when getting a missing session`() {
        every { repository.findById(any()) } returns Optional.empty()

        shouldThrow<KycSessionNotFoundException> { service.get(KycSessionId.generate()) }
    }

    @Test
    fun `should move an initiated session to screening`() {
        val id = KycSessionId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, KycStatus.INITIATED))
        val saved = slot<KycSessionEntity>()
        every { repository.saveAndFlush(capture(saved)) } answers { firstArg() }

        service.beginScreening(id).status shouldBe KycStatus.SCREENING
        saved.captured.status shouldBe KycStatus.SCREENING
    }

    @Test
    fun `should move a screening session to approved`() {
        val id = KycSessionId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, KycStatus.SCREENING))
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.approve(id).status shouldBe KycStatus.APPROVED
    }

    @Test
    fun `should move a screening session to rejected`() {
        val id = KycSessionId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, KycStatus.SCREENING))
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.reject(id).status shouldBe KycStatus.REJECTED
    }

    @Test
    fun `should reject an illegal transition`() {
        val id = KycSessionId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, KycStatus.INITIATED))

        shouldThrow<ComplianceDomainException> { service.approve(id) }
    }

    private fun entity(
        id: UUID,
        status: KycStatus,
    ) = KycSessionEntity(id, "subject-1", status, Instant.now(), 0)
}
