// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.AmlAlertStatus
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.domain.enum.KycStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StatusTransitionConsistencyTest {
    @Test
    fun `should mark a kyc status terminal exactly when it has no legal transition`() {
        KycStatus.entries.forEach { status ->
            status.isTerminal() shouldBe KycStatus.entries.none { status.canTransitionTo(it) }
        }
    }

    @Test
    fun `should mark a case status terminal exactly when it has no legal transition`() {
        CaseStatus.entries.forEach { status ->
            status.isTerminal() shouldBe CaseStatus.entries.none { status.canTransitionTo(it) }
        }
    }

    @Test
    fun `should mark an aml alert status terminal exactly when it has no legal transition`() {
        AmlAlertStatus.entries.forEach { status ->
            status.isTerminal() shouldBe AmlAlertStatus.entries.none { status.canTransitionTo(it) }
        }
    }

    @Test
    fun `should never allow a status to transition to itself`() {
        KycStatus.entries.forEach { it.canTransitionTo(it) shouldBe false }
        CaseStatus.entries.forEach { it.canTransitionTo(it) shouldBe false }
        AmlAlertStatus.entries.forEach { it.canTransitionTo(it) shouldBe false }
    }
}
