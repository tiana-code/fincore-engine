// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class AccountId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "acc_"

        fun generate(): AccountId = AccountId(PrefixedUlid.generate())

        fun fromString(raw: String): AccountId = AccountId(PrefixedUlid.parse(PREFIX, raw))
    }
}
