// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.TreeMap

/**
 * Deterministic, canonical hash of an evaluation input: a privacy-preserving identity of the facts a
 * decision was made on. Keys are sorted, decimals normalized (so 2.0 and 2.00 hash alike), and the result
 * is a SHA-256 hex digest matching the decision_logs input_hash shape.
 */
@Component
class InputHasher(
    private val objectMapper: ObjectMapper,
) {
    fun hash(input: EvaluationInput): String {
        val canonical = TreeMap<String, String>()
        input.attributes.forEach { (key, value) -> canonical[key] = canonicalValue(value) }
        return sha256Hex(objectMapper.writeValueAsString(canonical))
    }

    private fun canonicalValue(value: AttrValue): String =
        when (value) {
            is StringValue -> "s:${value.value}"
            is DecimalValue -> "d:${canonicalDecimal(value.value)}"
            is BoolValue -> "b:${value.value}"
        }

    private fun canonicalDecimal(value: BigDecimal): String = if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()

    private fun sha256Hex(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
