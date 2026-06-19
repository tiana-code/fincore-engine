// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.core.AmlRuleId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AmlRuleTest {
    @Test
    fun `should keep its rule key and default to enabled`() {
        val rule = AmlRule(AmlRuleId.generate(), "aml.structuring")
        rule.ruleKey shouldBe "aml.structuring"
        rule.enabled shouldBe true
    }

    @Test
    fun `should be disabled when created disabled`() {
        AmlRule(AmlRuleId.generate(), "aml.structuring", enabled = false).enabled shouldBe false
    }

    @Test
    fun `should reject a blank rule key`() {
        shouldThrow<ComplianceDomainException> { AmlRule(AmlRuleId.generate(), "") }
    }

    @Test
    fun `should reject a rule key with illegal characters`() {
        shouldThrow<ComplianceDomainException> { AmlRule(AmlRuleId.generate(), "has space") }
    }

    @Test
    fun `should accept a rule key at the maximum length`() {
        AmlRule(AmlRuleId.generate(), "a".repeat(128)).ruleKey.length shouldBe 128
    }

    @Test
    fun `should reject a rule key over the maximum length`() {
        shouldThrow<ComplianceDomainException> { AmlRule(AmlRuleId.generate(), "a".repeat(129)) }
    }
}
