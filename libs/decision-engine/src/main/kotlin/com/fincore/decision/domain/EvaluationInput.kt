// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.domain

@JvmInline
value class EvaluationInput(
    val attributes: Map<String, AttrValue>,
) {
    fun get(attr: String): AttrValue? = attributes[attr]
}
