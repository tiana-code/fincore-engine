// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.domain.ConditionTrace
import com.fincore.decision.domain.DecisionOutcome
import com.fincore.decision.domain.DecisionResult
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.EvaluationOutcome
import com.fincore.decision.store.application.EvaluationService
import com.fincore.decision.store.config.SecurityConfig
import com.fincore.decision.store.exception.EvaluationTimeoutException
import com.fincore.decision.store.exception.GlobalExceptionHandler
import com.fincore.decision.store.exception.InputNotMappableException
import com.fincore.decision.store.exception.RuleNotActiveException
import com.fincore.decision.store.exception.RuleNotFoundException
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(DecisionEvaluationController::class)
@Import(SecurityConfig::class, DecisionApiMapper::class, GlobalExceptionHandler::class, DecisionEvaluationControllerTest.Mocks::class)
class DecisionEvaluationControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val evaluationService: EvaluationService,
) {
    private val result =
        DecisionResult(true, DecisionOutcome("approve", listOf("LOW_RISK")), listOf(ConditionTrace("amount gte", true)))

    @TestConfiguration
    class Mocks {
        @Bean fun evaluationService(): EvaluationService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(evaluationService)
    }

    @Test
    fun `should reject an unauthenticated evaluate with 401`() {
        mockMvc
            .perform(post("/v1/decision/rules/k/evaluate").contentType(MediaType.APPLICATION_JSON).content("""{"amount":1}"""))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject an evaluate carrying only the read scope with 403`() {
        mockMvc
            .perform(
                post("/v1/decision/rules/k/evaluate")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"amount":1}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return the decision when the write scope is present`() {
        every { evaluationService.evaluate("k", any()) } returns EvaluationOutcome(result, UUID.randomUUID())

        mockMvc
            .perform(
                post("/v1/decision/rules/k/evaluate")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"amount":150}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.matched").value(true))
            .andExpect(jsonPath("$.outcome.label").value("approve"))
            .andExpect(jsonPath("$.outcome.reasonCodes[0]").value("LOW_RISK"))
            .andExpect(jsonPath("$.decisionLogId").exists())
            .andExpect(jsonPath("$.trace[0].description").value("amount gte"))
    }

    @Test
    fun `should map an unknown rule to a 404 problem document`() {
        every { evaluationService.evaluate("k", any()) } throws RuleNotFoundException("k")

        evaluatePost().andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"))
    }

    @Test
    fun `should map a rule with no active version to a 409 problem document`() {
        every { evaluationService.evaluate("k", any()) } throws RuleNotActiveException("k")

        evaluatePost().andExpect(status().isConflict).andExpect(jsonPath("$.code").value("RULE_NOT_ACTIVE"))
    }

    @Test
    fun `should map an unmappable input to a 422 problem document`() {
        every { evaluationService.evaluate("k", any()) } throws InputNotMappableException()

        evaluatePost().andExpect(status().isUnprocessableEntity).andExpect(jsonPath("$.code").value("INPUT_NOT_MAPPABLE"))
    }

    @Test
    fun `should map an evaluation timeout to a 503 problem document`() {
        every { evaluationService.evaluate("k", any()) } throws EvaluationTimeoutException(1)

        evaluatePost()
            .andExpect(status().isServiceUnavailable)
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("EVALUATION_TIMEOUT"))
    }

    private fun evaluatePost() =
        mockMvc.perform(
            post("/v1/decision/rules/k/evaluate")
                .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":1}"""),
        )

    private companion object {
        const val SCOPE_READ = "SCOPE_decision:read"
        const val SCOPE_WRITE = "SCOPE_decision:write"
        const val PROBLEM_JSON = "application/problem+json"
    }
}
