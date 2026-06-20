// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.core.ComplianceCaseId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ComplianceCaseTest {
    @Test
    fun `should start open when created`() {
        case().status shouldBe CaseStatus.OPEN
    }

    @Test
    fun `should resolve through claimed when transitioned legally`() {
        val case = case()
        case.transitionTo(CaseStatus.CLAIMED)
        case.transitionTo(CaseStatus.RESOLVED)
        case.status shouldBe CaseStatus.RESOLVED
        case.isTerminal() shouldBe true
    }

    @Test
    fun `should resolve through escalation when escalated then resolved`() {
        val case = case()
        case.transitionTo(CaseStatus.CLAIMED)
        case.transitionTo(CaseStatus.ESCALATED)
        case.transitionTo(CaseStatus.RESOLVED)
        case.status shouldBe CaseStatus.RESOLVED
    }

    @Test
    fun `should reject claiming a resolved case`() {
        val case = case()
        case.transitionTo(CaseStatus.CLAIMED)
        case.transitionTo(CaseStatus.RESOLVED)
        shouldThrow<ComplianceDomainException> { case.transitionTo(CaseStatus.CLAIMED) }
    }

    @Test
    fun `should reject a blank reference`() {
        shouldThrow<ComplianceDomainException> { ComplianceCase(ComplianceCaseId.generate(), "") }
    }

    private fun case(): ComplianceCase = ComplianceCase(ComplianceCaseId.generate(), "case-ref-1")
}
