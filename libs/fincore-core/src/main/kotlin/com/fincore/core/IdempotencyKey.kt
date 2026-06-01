// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class IdempotencyKey(
    val value: String,
) {
    init {
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "IdempotencyKey length must be $MIN_LENGTH-$MAX_LENGTH characters, got ${value.length}"
        }
        require(VALID_CHARS.matches(value)) {
            "IdempotencyKey must match [A-Za-z0-9_-]+, got: '$value'"
        }
    }

    companion object {
        private const val MIN_LENGTH = 32
        private const val MAX_LENGTH = 128
        private val VALID_CHARS = Regex("^[A-Za-z0-9_-]+$")

        fun of(raw: String): IdempotencyKey = IdempotencyKey(raw)

        fun generate(): IdempotencyKey = IdempotencyKey(UUID.randomUUID().toString())
    }
}
