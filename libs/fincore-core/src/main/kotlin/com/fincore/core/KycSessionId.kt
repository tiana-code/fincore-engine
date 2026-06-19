// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class KycSessionId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "kyc_"

        fun generate(): KycSessionId = KycSessionId(PrefixedUlid.generate())

        fun fromString(raw: String): KycSessionId = KycSessionId(PrefixedUlid.parse(PREFIX, raw))
    }
}
