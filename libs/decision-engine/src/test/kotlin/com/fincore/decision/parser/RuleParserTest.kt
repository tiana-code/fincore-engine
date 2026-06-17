// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.parser

import com.fincore.decision.domain.DecisionDslException
import com.fincore.decision.domain.DslErrorCode
import com.fincore.decision.domain.MatchesComparison
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RuleParserTest {
    private val parser = RuleParser()

    @Test
    fun `should reject unknown operator when op is not recognized`() {
        val ex =
            shouldThrow<DecisionDslException> {
                parser.parse("""{"condition":{"attr":"a","op":"like","value":1},"outcome":{"label":"x"}}""")
            }
        ex.code shouldBe DslErrorCode.UNKNOWN_OPERATOR
    }

    @Test
    fun `should reject empty logical group when child list is empty`() {
        val ex =
            shouldThrow<DecisionDslException> {
                parser.parse("""{"condition":{"all":[]},"outcome":{"label":"x"}}""")
            }
        ex.code shouldBe DslErrorCode.EMPTY_LOGICAL_GROUP
    }

    @Test
    fun `should reject type mismatch when ordered operator has a string value`() {
        val ex =
            shouldThrow<DecisionDslException> {
                parser.parse("""{"condition":{"attr":"a","op":"gt","value":"x"},"outcome":{"label":"x"}}""")
            }
        ex.code shouldBe DslErrorCode.TYPE_MISMATCH
    }

    @Test
    fun `should reject invalid pattern when matches regex is malformed`() {
        val ex =
            shouldThrow<DecisionDslException> {
                parser.parse("""{"condition":{"attr":"a","op":"matches","value":"["},"outcome":{"label":"x"}}""")
            }
        ex.code shouldBe DslErrorCode.INVALID_PATTERN
    }

    @Test
    fun `should reject missing field when condition is absent`() {
        val ex =
            shouldThrow<DecisionDslException> {
                parser.parse("""{"outcome":{"label":"x"}}""")
            }
        ex.code shouldBe DslErrorCode.MISSING_FIELD
    }

    @Test
    fun `should produce equal rules when parsing the same matches rule twice after forcing the lazy regex`() {
        val json = """{"condition":{"attr":"name","op":"matches","value":"a.*"},"outcome":{"label":"x"}}"""
        val first = parser.parse(json)
        (first.condition as MatchesComparison).regex.matches("abc") shouldBe true
        first shouldBe parser.parse(json)
    }
}
