// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class AmlRuleId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "rule_"

        fun generate(): AmlRuleId = AmlRuleId(PrefixedUlid.generate())

        fun fromString(raw: String): AmlRuleId = AmlRuleId(PrefixedUlid.parse(PREFIX, raw))
    }
}
