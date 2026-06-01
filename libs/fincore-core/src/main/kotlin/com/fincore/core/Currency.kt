// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

@JvmInline
value class Currency private constructor(
    val code: String,
) {
    val fractionDigits: Int
        get() = FRACTION_DIGITS[code] ?: 2

    override fun toString(): String = code

    companion object {
        private val CODE_REGEX = Regex("^[A-Z]{3}$")

        private val FRACTION_DIGITS: Map<String, Int> =
            mapOf(
                "BHD" to 3,
                "IQD" to 3,
                "JOD" to 3,
                "KWD" to 3,
                "LYD" to 3,
                "OMR" to 3,
                "TND" to 3,
                "JPY" to 0,
                "KRW" to 0,
                "VND" to 0,
                "HUF" to 0,
                "CLP" to 0,
                "ISK" to 0,
                "PYG" to 0,
                "RWF" to 0,
                "UGX" to 0,
                "EUR" to 2,
                "USD" to 2,
                "GBP" to 2,
                "CHF" to 2,
                "SEK" to 2,
                "NOK" to 2,
                "DKK" to 2,
                "PLN" to 2,
                "CZK" to 2,
                "CAD" to 2,
                "AUD" to 2,
                "NZD" to 2,
                "SGD" to 2,
                "HKD" to 2,
            )

        fun of(code: String): Currency {
            val trimmed = code.trim()
            require(CODE_REGEX.matches(trimmed)) {
                "Invalid ISO 4217 currency code: '$code'. Must be exactly 3 uppercase ASCII letters."
            }
            return Currency(trimmed)
        }

        val EUR = Currency("EUR")
        val USD = Currency("USD")
        val GBP = Currency("GBP")
        val CHF = Currency("CHF")
        val JPY = Currency("JPY")
        val SEK = Currency("SEK")
        val NOK = Currency("NOK")
        val DKK = Currency("DKK")
        val PLN = Currency("PLN")
        val CZK = Currency("CZK")
        val CAD = Currency("CAD")
        val AUD = Currency("AUD")
        val NZD = Currency("NZD")
        val SGD = Currency("SGD")
        val HKD = Currency("HKD")
    }
}
