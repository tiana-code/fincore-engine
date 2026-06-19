// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.AmlAlertStatus
import com.fincore.core.AmlAlertId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AmlAlertTest {
    @Test
    fun `should start open when created`() {
        alert().status shouldBe AmlAlertStatus.OPEN
    }

    @Test
    fun `should resolve when transitioned from open`() {
        val a = alert()
        a.transitionTo(AmlAlertStatus.RESOLVED)
        a.status shouldBe AmlAlertStatus.RESOLVED
        a.isTerminal() shouldBe true
    }

    @Test
    fun `should dismiss when transitioned from open`() {
        val a = alert()
        a.transitionTo(AmlAlertStatus.DISMISSED)
        a.status shouldBe AmlAlertStatus.DISMISSED
    }

    @Test
    fun `should reject reopening a resolved alert`() {
        val a = alert()
        a.transitionTo(AmlAlertStatus.RESOLVED)
        shouldThrow<ComplianceDomainException> { a.transitionTo(AmlAlertStatus.OPEN) }
    }

    @Test
    fun `should reject a rule key with illegal characters`() {
        shouldThrow<ComplianceDomainException> { AmlAlert(AmlAlertId.generate(), "bad key!") }
    }

    private fun alert(): AmlAlert = AmlAlert(AmlAlertId.generate(), "aml.velocity.daily")
}
