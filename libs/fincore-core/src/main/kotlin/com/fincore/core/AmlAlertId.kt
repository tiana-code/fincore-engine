// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.util.UUID

@JvmInline
value class AmlAlertId(
    val value: UUID,
) {
    override fun toString(): String = PrefixedUlid.format(PREFIX, value)

    companion object {
        private const val PREFIX = "alert_"

        fun generate(): AmlAlertId = AmlAlertId(PrefixedUlid.generate())

        fun fromString(raw: String): AmlAlertId = AmlAlertId(PrefixedUlid.parse(PREFIX, raw))
    }
}
