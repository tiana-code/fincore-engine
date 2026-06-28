// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.serialization

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyAmountSerializerTest {
    private data class Holder(
        @JsonSerialize(using = MoneyAmountSerializer::class) val amount: BigDecimal,
    )

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize amount as a json string preserving trailing zeros`() {
        mapper.writeValueAsString(Holder(BigDecimal("125.50"))) shouldBe """{"amount":"125.50"}"""
    }

    @Test
    fun `should serialize a high scale amount losslessly without scientific notation`() {
        val value = "12345678901234567890.123456789012345678"
        mapper.writeValueAsString(Holder(BigDecimal(value))) shouldBe """{"amount":"$value"}"""
    }
}
