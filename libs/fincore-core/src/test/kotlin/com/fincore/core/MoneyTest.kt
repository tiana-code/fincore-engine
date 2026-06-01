// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyTest {
    private val eur = Currency.of("EUR")
    private val usd = Currency.of("USD")

    @Test
    fun `should create money from string amount`() {
        val money = Money.of("100.00", eur)
        money.amount shouldBe BigDecimal("100.00")
        money.currency shouldBe eur
    }

    @Test
    fun `should create zero money`() {
        val money = Money.zero(eur)
        money.isZero().shouldBeTrue()
    }

    @Test
    fun `should reject scale greater than 18`() {
        shouldThrow<IllegalArgumentException> {
            Money(BigDecimal("0.0000000000000000001"), eur)
        }
    }

    @Test
    fun `should treat 1_0 and 1_00 as equal via compareTo`() {
        val left = Money.of("1.0", eur)
        val right = Money.of("1.00", eur)
        left.compareTo(right) shouldBe 0
    }

    @Test
    fun `isZero should return true for 0_00 despite non-default scale`() {
        val money = Money(BigDecimal("0.00"), eur)
        money.isZero().shouldBeTrue()
    }

    @Test
    fun `should add two same-currency amounts`() {
        val left = Money.of("100.00", eur)
        val right = Money.of("50.25", eur)
        val result = left + right
        result.amount.compareTo(BigDecimal("150.25")) shouldBe 0
        result.currency shouldBe eur
    }

    @Test
    fun `should subtract same-currency amounts`() {
        val left = Money.of("100.00", eur)
        val right = Money.of("30.50", eur)
        val result = left - right
        result.amount.compareTo(BigDecimal("69.50")) shouldBe 0
    }

    @Test
    fun `should multiply by scalar`() {
        val money = Money.of("10.00", eur)
        val result = money * BigDecimal("3")
        result.amount.compareTo(BigDecimal("30.00")) shouldBe 0
    }

    @Test
    fun `should divide by scalar`() {
        val money = Money.of("100.00", eur)
        val result = money / BigDecimal("4")
        result.amount.compareTo(BigDecimal("25.00")) shouldBe 0
    }

    @Test
    fun `should negate amount`() {
        val money = Money.of("50.00", eur)
        val neg = -money
        neg.isNegative().shouldBeTrue()
        neg.amount.compareTo(BigDecimal("-50.00")) shouldBe 0
    }

    @Test
    fun `should return absolute value`() {
        val money = Money.of("-50.00", eur)
        val abs = money.abs()
        abs.isPositive().shouldBeTrue()
        abs.amount.compareTo(BigDecimal("50.00")) shouldBe 0
    }

    @Test
    fun `should divide non-terminating decimal without exception using MATH_CONTEXT`() {
        val money = Money.of("100.00", eur)
        val result = money / BigDecimal("3")
        result.isPositive().shouldBeTrue()
    }

    @Test
    fun `should round 0_005 down to 0_00 with HALF_EVEN`() {
        val money = Money.of("0.005", eur)
        val rounded = money.amount.setScale(2, RoundingMode.HALF_EVEN)
        rounded shouldBe BigDecimal("0.00")
    }

    @Test
    fun `should round 0_015 up to 0_02 with HALF_EVEN`() {
        val money = Money.of("0.015", eur)
        val rounded = money.amount.setScale(2, RoundingMode.HALF_EVEN)
        rounded shouldBe BigDecimal("0.02")
    }

    @Test
    fun `should throw on adding different currencies`() {
        val euros = Money.of("100.00", eur)
        val dollars = Money.of("50.00", usd)
        shouldThrow<CurrencyMismatchException> { euros + dollars }
    }

    @Test
    fun `should throw on subtracting different currencies`() {
        val euros = Money.of("100.00", eur)
        val dollars = Money.of("50.00", usd)
        shouldThrow<CurrencyMismatchException> { euros - dollars }
    }

    @Test
    fun `should throw on comparing different currencies`() {
        val euros = Money.of("100.00", eur)
        val dollars = Money.of("100.00", usd)
        shouldThrow<CurrencyMismatchException> { euros.compareTo(dollars) }
    }

    @Test
    fun `should compare amounts correctly`() {
        val small = Money.of("10.00", eur)
        val large = Money.of("20.00", eur)
        small shouldBeLessThan large
        large shouldBeGreaterThan small
    }

    @Test
    fun `isPositive should be true for positive amount`() {
        Money.of("0.01", eur).isPositive().shouldBeTrue()
    }

    @Test
    fun `isNegative should be true for negative amount`() {
        Money.of("-0.01", eur).isNegative().shouldBeTrue()
    }

    @Test
    fun `isZero should be false for non-zero`() {
        Money.of("0.01", eur).isZero().shouldBeFalse()
    }

    @Test
    fun `format should use currency fraction digits`() {
        val money = Money.of("100", eur)
        val formatted = money.format()
        formatted.contains("EUR").shouldBeTrue()
    }
}
