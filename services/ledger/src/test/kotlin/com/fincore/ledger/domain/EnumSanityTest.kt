// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EnumSanityTest {
    @Test
    fun `AccountType should contain exactly the expected members`() {
        AccountType.entries.map { it.name } shouldContainExactlyInAnyOrder
            listOf(
                "ASSET",
                "LIABILITY",
                "EQUITY",
                "REVENUE",
                "EXPENSE",
                "USER_WALLET",
                "FEE",
                "RESERVE",
                "SUSPENSE",
            )
    }

    @Test
    fun `AccountStatus should contain exactly ACTIVE FROZEN CLOSED`() {
        AccountStatus.entries.map { it.name } shouldContainExactlyInAnyOrder
            listOf(
                "ACTIVE",
                "FROZEN",
                "CLOSED",
            )
    }

    @Test
    fun `TransactionStatus should contain exactly POSTED REVERSED`() {
        TransactionStatus.entries.map { it.name } shouldContainExactlyInAnyOrder
            listOf(
                "POSTED",
                "REVERSED",
            )
    }

    @Test
    fun `EntryDirection should contain exactly DEBIT CREDIT`() {
        EntryDirection.entries.map { it.name } shouldContainExactlyInAnyOrder
            listOf(
                "DEBIT",
                "CREDIT",
            )
    }

    @Test
    fun `each enum value name should equal its toString so no custom toString breaks DB column round-trip`() {
        AccountType.entries.forEach { it.name shouldBe it.toString() }
        AccountStatus.entries.forEach { it.name shouldBe it.toString() }
        TransactionStatus.entries.forEach { it.name shouldBe it.toString() }
        EntryDirection.entries.forEach { it.name shouldBe it.toString() }
    }
}
