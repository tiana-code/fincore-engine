// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class PaymentId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "pay_"

        fun generate(): PaymentId = PaymentId(PrefixedUlid.generate())

        fun fromString(raw: String): PaymentId = PaymentId(PrefixedUlid.parse(PREFIX, raw))
    }
}
