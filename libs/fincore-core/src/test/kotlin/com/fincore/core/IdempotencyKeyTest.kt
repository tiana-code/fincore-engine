// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IdempotencyKeyTest {
    @Test
    fun `should accept valid 32-character key`() {
        val key = IdempotencyKey("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
        key.value shouldBe "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
    }

    @Test
    fun `should accept valid 128-character key`() {
        val key = IdempotencyKey("a".repeat(128))
        key.value.length shouldBe 128
    }

    @Test
    fun `should reject key shorter than 32 characters`() {
        shouldThrow<IllegalArgumentException> { IdempotencyKey("short") }
    }

    @Test
    fun `should reject key longer than 128 characters`() {
        shouldThrow<IllegalArgumentException> {
            IdempotencyKey("a".repeat(129))
        }
    }

    @Test
    fun `should reject key with forbidden characters`() {
        shouldThrow<IllegalArgumentException> {
            IdempotencyKey("a".repeat(31) + " ")
        }
    }

    @Test
    fun `should accept hyphen and underscore in key`() {
        val key = IdempotencyKey("a1b2-c3d4_e5f6a1b2c3d4e5f6a1b2c3d4")
        key.value.length shouldBe 34
    }

    @Test
    fun `two keys with same value should be equal`() {
        val value = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
        IdempotencyKey(value) shouldBe IdempotencyKey(value)
    }

    @Test
    fun `generate should produce a key of valid length`() {
        val key = IdempotencyKey.generate()
        key.value.length shouldBe 36
    }
}
