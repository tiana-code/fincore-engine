// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.util.UUID

class AccountIdTest {
    @Test
    fun `should create AccountId from UUID`() {
        val uuid = UUID.randomUUID()
        val id = AccountId(uuid)
        id.value shouldBe uuid
    }

    @Test
    fun `should serialize to acc_ prefix with Crockford base32 ULID body`() {
        val text = AccountId.generate().toString()
        text shouldStartWith "acc_"
        text.length shouldBe 30
        text.removePrefix("acc_") shouldMatch Regex("^[0-9A-HJKMNP-TV-Z]{26}$")
    }

    @Test
    fun `should parse from string representation`() {
        val uuid = UUID.randomUUID()
        val id = AccountId(uuid)
        val parsed = AccountId.fromString(id.toString())
        parsed shouldBe id
    }

    @Test
    fun `should generate unique ids`() {
        val id1 = AccountId.generate()
        val id2 = AccountId.generate()
        (id1 == id2) shouldBe false
    }

    @Test
    fun `two AccountIds with same UUID should be equal`() {
        val uuid = UUID.randomUUID()
        AccountId(uuid) shouldBe AccountId(uuid)
    }
}
