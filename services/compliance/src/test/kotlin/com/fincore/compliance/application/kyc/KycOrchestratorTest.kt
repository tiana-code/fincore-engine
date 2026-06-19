// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.ComplianceDomainException
import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.core.KycSessionId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class KycOrchestratorTest {
    private val service = mockk<KycService>()
    private val provider = mockk<KycProvider>()
    private val orchestrator = KycOrchestrator(service, provider)

    private val id = KycSessionId.generate()

    @Test
    fun `should approve when the provider approves`() {
        screeningReturns()
        every { provider.check(any()) } returns KycCheckResult.Approved("ref-1")
        every { service.approve(id) } returns session(KycStatus.APPROVED)

        orchestrator.process(id).status shouldBe KycStatus.APPROVED
        verify { provider.check(any()) }
    }

    @Test
    fun `should reject when the provider rejects`() {
        screeningReturns()
        every { provider.check(any()) } returns KycCheckResult.Rejected("declined")
        every { service.reject(id) } returns session(KycStatus.REJECTED)

        orchestrator.process(id).status shouldBe KycStatus.REJECTED
    }

    @Test
    fun `should stay screening when the provider is pending`() {
        screeningReturns()
        every { provider.check(any()) } returns KycCheckResult.Pending("ref-2")

        orchestrator.process(id).status shouldBe KycStatus.SCREENING
        verify(exactly = 0) { service.approve(any()) }
        verify(exactly = 0) { service.reject(any()) }
    }

    @Test
    fun `should stay screening when the provider has insufficient data`() {
        screeningReturns()
        every { provider.check(any()) } returns KycCheckResult.InsufficientData(listOf("attr-a"))

        orchestrator.process(id).status shouldBe KycStatus.SCREENING
        verify(exactly = 0) { service.approve(any()) }
        verify(exactly = 0) { service.reject(any()) }
    }

    @Test
    fun `should propagate a provider exception`() {
        screeningReturns()
        every { provider.check(any()) } throws KycProviderException("upstream unavailable")

        shouldThrow<KycProviderException> { orchestrator.process(id) }
    }

    @Test
    fun `should propagate a domain exception when the session is already terminal at decision`() {
        screeningReturns()
        every { provider.check(any()) } returns KycCheckResult.Approved("ref-1")
        every { service.approve(id) } throws ComplianceDomainException("illegal transition")

        shouldThrow<ComplianceDomainException> { orchestrator.process(id) }
    }

    private fun screeningReturns() {
        every { service.beginScreening(id) } returns session(KycStatus.SCREENING)
    }

    private fun session(status: KycStatus) = KycSession(id, "subject-1", status)
}
