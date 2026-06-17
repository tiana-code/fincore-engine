// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

import java.math.BigDecimal

enum class ValueKind { STRING, DECIMAL, BOOLEAN }

sealed interface AttrValue {
    val kind: ValueKind

    fun semanticallyEquals(other: AttrValue): Boolean =
        when {
            this is DecimalValue && other is DecimalValue -> value.compareTo(other.value) == 0
            this is StringValue && other is StringValue -> value == other.value
            this is BoolValue && other is BoolValue -> value == other.value
            else -> false
        }
}

data class StringValue(
    val value: String,
) : AttrValue {
    override val kind: ValueKind get() = ValueKind.STRING
}

data class DecimalValue(
    val value: BigDecimal,
) : AttrValue {
    override val kind: ValueKind get() = ValueKind.DECIMAL
}

data class BoolValue(
    val value: Boolean,
) : AttrValue {
    override val kind: ValueKind get() = ValueKind.BOOLEAN
}
