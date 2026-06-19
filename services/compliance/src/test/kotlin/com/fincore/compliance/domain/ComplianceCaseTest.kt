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
        val c = case()
        c.transitionTo(CaseStatus.CLAIMED)
        c.transitionTo(CaseStatus.RESOLVED)
        c.status shouldBe CaseStatus.RESOLVED
        c.isTerminal() shouldBe true
    }

    @Test
    fun `should resolve through escalation when escalated then resolved`() {
        val c = case()
        c.transitionTo(CaseStatus.CLAIMED)
        c.transitionTo(CaseStatus.ESCALATED)
        c.transitionTo(CaseStatus.RESOLVED)
        c.status shouldBe CaseStatus.RESOLVED
    }

    @Test
    fun `should reject claiming a resolved case`() {
        val c = case()
        c.transitionTo(CaseStatus.CLAIMED)
        c.transitionTo(CaseStatus.RESOLVED)
        shouldThrow<ComplianceDomainException> { c.transitionTo(CaseStatus.CLAIMED) }
    }

    @Test
    fun `should reject a blank reference`() {
        shouldThrow<ComplianceDomainException> { ComplianceCase(ComplianceCaseId.generate(), "") }
    }

    private fun case(): ComplianceCase = ComplianceCase(ComplianceCaseId.generate(), "case-ref-1")
}
