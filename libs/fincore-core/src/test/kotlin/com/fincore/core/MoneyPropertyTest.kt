// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bigDecimal
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.math.RoundingMode

class MoneyPropertyTest :
    FreeSpec({

        fun arbMoney(currency: Currency): Arb<Money> =
            Arb
                .bigDecimal(
                    min = BigDecimal("-999999999999999999"),
                    max = BigDecimal("999999999999999999"),
                ).map { amount ->
                    val scaled = amount.setScale(6, RoundingMode.HALF_EVEN)
                    Money(scaled, currency)
                }

        "commutativity: a + b == b + a" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency), arbMoney(currency)) { left, right ->
                val leftThenRight = left + right
                val rightThenLeft = right + left
                (leftThenRight.amount.compareTo(rightThenLeft.amount) == 0).shouldBeTrue()
            }
        }

        "associativity: (a + b) + c == a + (b + c)" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency), arbMoney(currency), arbMoney(currency)) { first, second, third ->
                val left = (first + second) + third
                val right = first + (second + third)
                (left.amount.compareTo(right.amount) == 0).shouldBeTrue()
            }
        }

        "identity: a + zero == a" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency)) { value ->
                val result = value + Money.zero(currency)
                (result.amount.compareTo(value.amount) == 0).shouldBeTrue()
            }
        }

        "double negation: -(-a) == a" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency)) { value ->
                val result = -(-value)
                (result.amount.compareTo(value.amount) == 0).shouldBeTrue()
            }
        }

        "addition inverse: a + (-a) == zero" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency)) { value ->
                val result = value + (-value)
                result.isZero().shouldBeTrue()
            }
        }

        "currency preservation: all arithmetic ops preserve currency" {
            val currency = Currency.of("EUR")
            checkAll(arbMoney(currency), arbMoney(currency)) { left, right ->
                (left + right).currency shouldBe currency
                (left - right).currency shouldBe currency
                (left * BigDecimal("2")).currency shouldBe currency
                (-left).currency shouldBe currency
                left.abs().currency shouldBe currency
            }
        }

        "scale invariance: same numeric value with different scale compares equal" {
            val currency = Currency.of("EUR")
            Arb
                .element(
                    listOf("1.0", "1.00", "1.000", "1.0000"),
                ).let { arb ->
                    checkAll(arb, arb) { firstScaleStr, secondScaleStr ->
                        val first = Money.of(firstScaleStr, currency)
                        val second = Money.of(secondScaleStr, currency)
                        (first.compareTo(second) == 0).shouldBeTrue()
                    }
                }
        }
    })
