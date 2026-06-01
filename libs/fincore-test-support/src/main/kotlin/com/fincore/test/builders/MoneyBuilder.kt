// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.builders

import com.fincore.core.Currency
import com.fincore.core.Money
import java.math.BigDecimal

class MoneyBuilder {
    private var amount: BigDecimal = BigDecimal.ZERO
    private var currency: Currency = Currency.EUR

    fun amount(value: String): MoneyBuilder = apply { amount = BigDecimal(value) }

    fun amount(value: BigDecimal): MoneyBuilder = apply { this.amount = value }

    fun amount(value: Long): MoneyBuilder = apply { amount = BigDecimal.valueOf(value) }

    fun currency(c: Currency): MoneyBuilder = apply { currency = c }

    fun currency(code: String): MoneyBuilder = apply { currency = Currency.of(code) }

    fun build(): Money = Money.of(amount, currency)

    companion object {
        fun aMoney(): MoneyBuilder = MoneyBuilder()

        fun anEur(amount: String): Money = aMoney().amount(amount).currency(Currency.EUR).build()

        fun aUsd(amount: String): Money = aMoney().amount(amount).currency(Currency.USD).build()
    }
}
