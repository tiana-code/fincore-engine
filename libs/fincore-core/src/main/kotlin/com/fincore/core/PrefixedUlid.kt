// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import java.util.UUID

// Prefixed ULID identifier codec - see ADR-0010 for the rationale.
internal object PrefixedUlid {
    fun generate(): UUID = UlidCreator.getMonotonicUlid().toUuid()

    fun format(
        prefix: String,
        value: UUID,
    ): String = prefix + Ulid.from(value).toString()

    fun parse(
        prefix: String,
        raw: String,
    ): UUID {
        require(raw.startsWith(prefix)) {
            "Invalid identifier: '$raw'. Expected prefix '$prefix'."
        }
        val encoded = raw.removePrefix(prefix)
        require(Ulid.isValid(encoded)) {
            "Invalid identifier: '$raw'. Body is not a valid ULID."
        }
        return Ulid.from(encoded).toUuid()
    }
}
