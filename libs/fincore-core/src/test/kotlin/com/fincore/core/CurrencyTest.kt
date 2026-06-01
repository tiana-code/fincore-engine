// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CurrencyTest {
    @Test
    fun `should create currency from valid ISO 4217 code`() {
        val eur = Currency.of("EUR")
        eur.code shouldBe "EUR"
    }

    @Test
    fun `should reject lowercase currency code`() {
        shouldThrow<IllegalArgumentException> { Currency.of("eur") }
    }

    @Test
    fun `should reject non-3-letter code`() {
        shouldThrow<IllegalArgumentException> { Currency.of("EU") }
        shouldThrow<IllegalArgumentException> { Currency.of("EURO") }
    }

    @Test
    fun `should reject empty code`() {
        shouldThrow<IllegalArgumentException> { Currency.of("") }
    }

    @Test
    fun `should reject code with digits`() {
        shouldThrow<IllegalArgumentException> { Currency.of("EU1") }
    }

    @Test
    fun `EUR should have 2 fraction digits`() {
        Currency.EUR.fractionDigits shouldBe 2
    }

    @Test
    fun `USD should have 2 fraction digits`() {
        Currency.USD.fractionDigits shouldBe 2
    }

    @Test
    fun `GBP should have 2 fraction digits`() {
        Currency.GBP.fractionDigits shouldBe 2
    }

    @Test
    fun `JPY should have 0 fraction digits`() {
        Currency.JPY.fractionDigits shouldBe 0
    }

    @Test
    fun `two currencies with same code should be equal`() {
        Currency.of("EUR") shouldBe Currency.of("EUR")
    }

    @Test
    fun `two currencies with different codes should not be equal`() {
        (Currency.of("EUR") == Currency.of("USD")) shouldBe false
    }

    @Test
    fun `toString should return the currency code`() {
        Currency.EUR.toString() shouldBe "EUR"
    }
}
