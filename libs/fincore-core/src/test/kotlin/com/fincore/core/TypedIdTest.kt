// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class TypedIdTest {
    @Test
    fun `should round-trip TransactionId with tx_ prefix`() {
        val id = TransactionId.generate()
        id.toString() shouldStartWith "tx_"
        TransactionId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should round-trip EntryId with ent_ prefix`() {
        val id = EntryId.generate()
        id.toString() shouldStartWith "ent_"
        EntryId.fromString(id.toString()) shouldBe id
    }

    @Test
    fun `should round-trip PaymentId with pay_ prefix`() {
        val id = PaymentId.generate()
        id.toString() shouldStartWith "pay_"
        PaymentId.fromString(id.toString()) shouldBe id
    }
}
