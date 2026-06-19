// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.observability

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class PiiMaskerTest {
    @Test
    fun `should mask an email address`() {
        val masked = PiiMasker.mask("contact user@example.test now")

        masked shouldNotContain "user@example.test"
        masked shouldContain PiiMasker.REDACTION
    }

    @Test
    fun `should mask a long digit run in the pan range`() {
        val masked = PiiMasker.mask("card 4111111111111111 used")

        masked shouldNotContain "4111111111111111"
        masked shouldContain PiiMasker.REDACTION
    }

    @Test
    fun `should leave a short digit sequence intact`() {
        PiiMasker.mask("order 12345 placed") shouldBe "order 12345 placed"
    }

    @Test
    fun `should leave a twelve digit run intact so uuid node groups survive`() {
        PiiMasker.mask("node 426614174000 here") shouldBe "node 426614174000 here"
    }

    @Test
    fun `should mask a long digit run beyond the pan upper bound`() {
        val masked = PiiMasker.mask("ref 123456789012345678901234 end")

        masked shouldNotContain "123456789012345678901234"
        masked shouldContain PiiMasker.REDACTION
    }

    @Test
    fun `should mask a bearer token`() {
        val masked = PiiMasker.mask("Authorization: Bearer faketoken_aBcD1234efGh")

        masked shouldNotContain "faketoken_aBcD1234efGh"
        masked shouldContain PiiMasker.REDACTION
    }

    @Test
    fun `should leave a bearer token with a too-short body intact`() {
        PiiMasker.mask("Authorization: Bearer tok") shouldBe "Authorization: Bearer tok"
    }

    @Test
    fun `should leave a uuid correlation id intact`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"

        PiiMasker.mask("correlation $uuid") shouldBe "correlation $uuid"
    }

    @Test
    fun `should be idempotent when re-masking already-masked text`() {
        val once = PiiMasker.mask("email user@example.test")

        PiiMasker.mask(once) shouldBe once
    }

    @Test
    fun `should return the input unchanged when nothing matches`() {
        PiiMasker.mask("a plain log line") shouldBe "a plain log line"
    }
}
