// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fincore.decision.domain.AttrValue
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.InputNotMappableException
import com.fincore.decision.store.exception.InputTooLargeException
import org.springframework.stereotype.Component

@Component
class InputMapper(
    private val properties: DecisionApiProperties,
) {
    fun toEvaluationInput(attributes: Map<String, JsonNode>): EvaluationInput {
        if (attributes.size > properties.maxInputAttributes) throw InputTooLargeException(properties.maxInputAttributes)
        return EvaluationInput(attributes.mapValues { (_, node) -> toAttrValue(node) })
    }

    private fun toAttrValue(node: JsonNode): AttrValue =
        when {
            node.isTextual -> StringValue(boundedText(node.textValue()))
            node.isBoolean -> BoolValue(node.booleanValue())
            node.isNumber -> DecimalValue(node.decimalValue())
            else -> throw InputNotMappableException()
        }

    private fun boundedText(value: String): String {
        if (value.length > properties.maxInputValueChars) throw InputTooLargeException(properties.maxInputValueChars)
        return value
    }
}
