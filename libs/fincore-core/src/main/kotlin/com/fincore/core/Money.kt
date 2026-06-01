// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) : Comparable<Money> {
    init {
        require(amount.scale() <= MAX_SCALE) {
            "Money amount scale must be <= $MAX_SCALE, got ${amount.scale()}"
        }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.add(other.amount, MATH_CONTEXT), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.subtract(other.amount, MATH_CONTEXT), currency)
    }

    operator fun times(scalar: BigDecimal): Money = Money(amount.multiply(scalar, MATH_CONTEXT), currency)

    operator fun times(scalar: Int): Money = times(BigDecimal.valueOf(scalar.toLong()))

    operator fun div(scalar: BigDecimal): Money {
        val quotient = amount.divide(scalar, MATH_CONTEXT)
        val bounded =
            if (quotient.scale() > MAX_SCALE) {
                quotient.setScale(MAX_SCALE, RoundingMode.HALF_EVEN)
            } else {
                quotient
            }
        return Money(bounded, currency)
    }

    operator fun unaryMinus(): Money = Money(amount.negate(), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    fun isPositive(): Boolean = amount > BigDecimal.ZERO

    fun isNegative(): Boolean = amount < BigDecimal.ZERO

    fun abs(): Money = Money(amount.abs(), currency)

    fun negate(): Money = -this

    fun withScale(scale: Int): Money = Money(amount.setScale(scale, RoundingMode.HALF_EVEN), currency)

    fun format(): String {
        val scaled = amount.setScale(currency.fractionDigits, RoundingMode.HALF_EVEN)
        return "${currency.code} ${scaled.toPlainString()}"
    }

    private fun requireSameCurrency(other: Money) {
        if (currency != other.currency) throw CurrencyMismatchException(currency, other.currency)
    }

    companion object {
        private const val MAX_SCALE = 18
        private val MATH_CONTEXT = MathContext.DECIMAL128

        fun of(
            amount: BigDecimal,
            currency: Currency,
        ): Money = Money(amount, currency)

        fun of(
            amount: String,
            currency: Currency,
        ): Money = Money(BigDecimal(amount), currency)

        fun of(
            amount: Long,
            currency: Currency,
        ): Money = Money(BigDecimal.valueOf(amount), currency)

        fun zero(currency: Currency): Money = Money(BigDecimal.ZERO, currency)
    }
}
