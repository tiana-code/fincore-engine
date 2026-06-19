// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

internal object RuleKey {
    private val PATTERN = Regex("^[A-Za-z0-9_.:-]{1,128}$")
    private const val DESCRIPTION = "1-128 characters from [A-Za-z0-9_.:-]"

    fun validate(
        value: String,
        field: String,
    ): String {
        if (!PATTERN.matches(value)) {
            throw ComplianceDomainException("$field must be $DESCRIPTION")
        }
        return value
    }
}
