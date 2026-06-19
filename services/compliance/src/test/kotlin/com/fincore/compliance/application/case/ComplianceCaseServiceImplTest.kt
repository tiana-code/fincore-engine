// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.compliance.domain.ComplianceDomainException
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.infrastructure.persistence.ComplianceCaseEntity
import com.fincore.compliance.infrastructure.persistence.ComplianceCasePersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.ComplianceCaseRepository
import com.fincore.core.ComplianceCaseId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

class ComplianceCaseServiceImplTest {
    private val repository = mockk<ComplianceCaseRepository>()
    private val service = ComplianceCaseServiceImpl(repository, ComplianceCasePersistenceAdapter())

    @Test
    fun `should open a case in the open state`() {
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.open(OpenCaseCommand("case-ref-1")).status shouldBe CaseStatus.OPEN
    }

    @Test
    fun `should throw not found when getting a missing case`() {
        every { repository.findById(any()) } returns Optional.empty()

        shouldThrow<CaseNotFoundException> { service.get(ComplianceCaseId.generate()) }
    }

    @Test
    fun `should claim an open case`() {
        val id = ComplianceCaseId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, CaseStatus.OPEN))
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.claim(id).status shouldBe CaseStatus.CLAIMED
    }

    @Test
    fun `should escalate a claimed case then resolve it`() {
        val id = ComplianceCaseId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, CaseStatus.CLAIMED))
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        service.escalate(id).status shouldBe CaseStatus.ESCALATED
    }

    @Test
    fun `should reject claiming a resolved case`() {
        val id = ComplianceCaseId.generate()
        every { repository.findById(id.value) } returns Optional.of(entity(id.value, CaseStatus.RESOLVED))

        shouldThrow<ComplianceDomainException> { service.claim(id) }
    }

    @Test
    fun `should list cases by status`() {
        every { repository.findByStatus(CaseStatus.OPEN) } returns listOf(entity(UUID.randomUUID(), CaseStatus.OPEN))

        service.list(CaseStatus.OPEN).size shouldBe 1
    }

    private fun entity(
        id: UUID,
        status: CaseStatus,
    ) = ComplianceCaseEntity(id, "case-ref-1", status, Instant.now(), 0)
}
