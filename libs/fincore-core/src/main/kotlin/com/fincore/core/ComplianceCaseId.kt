// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class ComplianceCaseId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "case_"

        fun generate(): ComplianceCaseId = ComplianceCaseId(PrefixedUlid.generate())

        fun fromString(raw: String): ComplianceCaseId = ComplianceCaseId(PrefixedUlid.parse(PREFIX, raw))
    }
}
