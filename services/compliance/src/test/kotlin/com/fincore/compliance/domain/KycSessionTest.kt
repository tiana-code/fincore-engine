// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.core.KycSessionId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KycSessionTest {
    @Test
    fun `should start in initiated when created`() {
        session().status shouldBe KycStatus.INITIATED
    }

    @Test
    fun `should advance through screening to approved when transitioned legally`() {
        val s = session()
        s.transitionTo(KycStatus.SCREENING)
        s.transitionTo(KycStatus.APPROVED)
        s.status shouldBe KycStatus.APPROVED
        s.isTerminal() shouldBe true
    }

    @Test
    fun `should reject an illegal transition`() {
        shouldThrow<ComplianceDomainException> { session().transitionTo(KycStatus.APPROVED) }
    }

    @Test
    fun `should reject a blank subject reference`() {
        shouldThrow<ComplianceDomainException> { KycSession(KycSessionId.generate(), " ") }
    }

    @Test
    fun `should reject a subject reference over the length limit`() {
        shouldThrow<ComplianceDomainException> { KycSession(KycSessionId.generate(), "x".repeat(141)) }
    }

    private fun session(): KycSession = KycSession(KycSessionId.generate(), "subject-1")
}
