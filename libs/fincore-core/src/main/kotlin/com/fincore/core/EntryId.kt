// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class EntryId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "ent_"

        fun generate(): EntryId = EntryId(PrefixedUlid.generate())

        fun fromString(raw: String): EntryId = EntryId(PrefixedUlid.parse(PREFIX, raw))
    }
}
