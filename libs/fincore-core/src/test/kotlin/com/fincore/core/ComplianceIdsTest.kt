// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class ComplianceIdsTest {
    @Test
    fun `should round-trip a kyc session id through its prefixed string`() {
        val id = KycSessionId.generate()

        id.toString() shouldStartWith "kyc_"
        KycSessionId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should round-trip an aml alert id through its prefixed string`() {
        val id = AmlAlertId.generate()

        id.toString() shouldStartWith "alert_"
        AmlAlertId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should round-trip a compliance case id through its prefixed string`() {
        val id = ComplianceCaseId.generate()

        id.toString() shouldStartWith "case_"
        ComplianceCaseId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should round-trip an aml rule id through its prefixed string`() {
        val id = AmlRuleId.generate()

        id.toString() shouldStartWith "rule_"
        AmlRuleId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should reject a kyc session id with the wrong prefix`() {
        shouldThrow<IllegalArgumentException> { KycSessionId.fromString(AmlAlertId.generate().toString()) }
    }

    @Test
    fun `should reject an aml alert id with the wrong prefix`() {
        shouldThrow<IllegalArgumentException> { AmlAlertId.fromString(KycSessionId.generate().toString()) }
    }

    @Test
    fun `should reject a compliance case id with the wrong prefix`() {
        shouldThrow<IllegalArgumentException> { ComplianceCaseId.fromString(AmlRuleId.generate().toString()) }
    }

    @Test
    fun `should reject an aml rule id with the wrong prefix`() {
        shouldThrow<IllegalArgumentException> { AmlRuleId.fromString(ComplianceCaseId.generate().toString()) }
    }
}
