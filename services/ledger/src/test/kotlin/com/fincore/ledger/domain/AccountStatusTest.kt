// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountStatus.ACTIVE
import com.fincore.ledger.domain.enum.AccountStatus.CLOSED
import com.fincore.ledger.domain.enum.AccountStatus.FROZEN
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AccountStatusTest {
    @Test
    fun `should have exactly three members ACTIVE FROZEN CLOSED`() {
        val members = AccountStatus.entries.map { it.name }
        members shouldBe listOf("ACTIVE", "FROZEN", "CLOSED")
    }

    @Test
    fun `should allow transition from ACTIVE to FROZEN`() {
        ACTIVE.canTransitionTo(FROZEN) shouldBe true
    }

    @Test
    fun `should allow transition from FROZEN to ACTIVE`() {
        FROZEN.canTransitionTo(ACTIVE) shouldBe true
    }

    @Test
    fun `should allow transition from ACTIVE to CLOSED`() {
        ACTIVE.canTransitionTo(CLOSED) shouldBe true
    }

    @Test
    fun `should allow transition from FROZEN to CLOSED`() {
        FROZEN.canTransitionTo(CLOSED) shouldBe true
    }

    @Test
    fun `should reject transition from CLOSED to any status`() {
        CLOSED.canTransitionTo(ACTIVE) shouldBe false
        CLOSED.canTransitionTo(FROZEN) shouldBe false
        CLOSED.canTransitionTo(CLOSED) shouldBe false
    }

    @Test
    fun `should reject transition from ACTIVE to ACTIVE`() {
        ACTIVE.canTransitionTo(ACTIVE) shouldBe false
    }

    @Test
    fun `should reject transition from FROZEN to FROZEN`() {
        FROZEN.canTransitionTo(FROZEN) shouldBe false
    }
}
