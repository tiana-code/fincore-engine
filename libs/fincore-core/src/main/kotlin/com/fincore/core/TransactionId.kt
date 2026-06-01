// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class TransactionId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "tx_"

        fun generate(): TransactionId = TransactionId(PrefixedUlid.generate())

        fun fromString(raw: String): TransactionId = TransactionId(PrefixedUlid.parse(PREFIX, raw))
    }
}
