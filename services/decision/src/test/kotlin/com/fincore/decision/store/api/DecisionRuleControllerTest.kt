// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fincore.decision.domain.DslErrorCode
import com.fincore.decision.store.api.mapper.DecisionApiMapper
import com.fincore.decision.store.application.ActiveVersionView
import com.fincore.decision.store.application.RuleAdminService
import com.fincore.decision.store.application.RuleDetailView
import com.fincore.decision.store.application.RuleView
import com.fincore.decision.store.config.SecurityConfig
import com.fincore.decision.store.exception.DslTooLargeException
import com.fincore.decision.store.exception.DuplicateRuleKeyException
import com.fincore.decision.store.exception.GlobalExceptionHandler
import com.fincore.decision.store.exception.InvalidRuleDslException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(DecisionRuleController::class)
@Import(SecurityConfig::class, DecisionApiMapper::class, GlobalExceptionHandler::class, DecisionRuleControllerTest.Mocks::class)
class DecisionRuleControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val ruleAdminService: RuleAdminService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun ruleAdminService(): RuleAdminService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(ruleAdminService)
    }

    @Test
    fun `should reject an unauthenticated create with 401`() {
        mockMvc
            .perform(post("/v1/decision/rules").contentType(MediaType.APPLICATION_JSON).content("""{"ruleKey":"k"}"""))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject a create carrying only the read scope with 403`() {
        mockMvc
            .perform(
                post("/v1/decision/rules")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ruleKey":"k"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should create a rule when the write scope is present`() {
        every { ruleAdminService.createRule("k") } returns RuleView(UUID.randomUUID(), "k")

        mockMvc
            .perform(
                post("/v1/decision/rules")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ruleKey":"k"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.ruleKey").value("k"))
    }

    @Test
    fun `should reject a blank rule key with a 400 problem document`() {
        mockMvc
            .perform(
                post("/v1/decision/rules")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ruleKey":""}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `should map a duplicate rule key to a 409 problem document`() {
        every { ruleAdminService.createRule("k") } throws DuplicateRuleKeyException("k")

        mockMvc
            .perform(
                post("/v1/decision/rules")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ruleKey":"k"}"""),
            ).andExpect(status().isConflict)
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("DUPLICATE_RULE_KEY"))
    }

    @Test
    fun `should map an invalid dsl to a 422 problem document carrying the dsl code`() {
        every { ruleAdminService.publishVersion("k", any()) } throws
            InvalidRuleDslException(DslErrorCode.UNKNOWN_OPERATOR, "unknown operator")

        mockMvc
            .perform(
                post("/v1/decision/rules/k/versions")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"condition":{"attr":"a","op":"bad","value":1},"outcome":{"label":"x"}}"""),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("INVALID_DSL"))
            .andExpect(jsonPath("$.dslCode").value("UNKNOWN_OPERATOR"))
    }

    @Test
    fun `should map an oversized dsl to a 422 problem document`() {
        every { ruleAdminService.publishVersion("k", any()) } throws DslTooLargeException(MAX_DSL)

        mockMvc
            .perform(
                post("/v1/decision/rules/k/versions")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"condition":{"attr":"a","op":"eq","value":1},"outcome":{"label":"x"}}"""),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("DSL_TOO_LARGE"))
    }

    @Test
    fun `should map a missing rule to a 404 problem document when publishing`() {
        every { ruleAdminService.publishVersion("k", any()) } throws RuleNotFoundException("k")

        mockMvc
            .perform(
                post("/v1/decision/rules/k/versions")
                    .with(jwt().authorities(SimpleGrantedAuthority(SCOPE_WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"condition":{"attr":"a","op":"eq","value":1},"outcome":{"label":"x"}}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"))
    }

    @Test
    fun `should return the active version with the read scope when the rule is read`() {
        every { ruleAdminService.getRule("k") } returns
            RuleDetailView(
                UUID.randomUUID(),
                "k",
                ActiveVersionView(1, """{"condition":{"attr":"a","op":"eq","value":1},"outcome":{"label":"approve"}}"""),
            )

        mockMvc
            .perform(get("/v1/decision/rules/k").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ruleKey").value("k"))
            .andExpect(jsonPath("$.activeVersion.versionNo").value(1))
            .andExpect(jsonPath("$.activeVersion.dsl.outcome.label").value("approve"))
    }

    @Test
    fun `should return a null active version when the rule has none`() {
        every { ruleAdminService.getRule("k") } returns RuleDetailView(UUID.randomUUID(), "k", null)

        mockMvc
            .perform(get("/v1/decision/rules/k").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activeVersion").doesNotExist())
    }

    @Test
    fun `should map a missing rule to a 404 problem document when read`() {
        every { ruleAdminService.getRule("x") } throws RuleNotFoundException("x")

        mockMvc
            .perform(get("/v1/decision/rules/x").with(jwt().authorities(SimpleGrantedAuthority(SCOPE_READ))))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"))
    }

    private companion object {
        const val SCOPE_READ = "SCOPE_decision:read"
        const val SCOPE_WRITE = "SCOPE_decision:write"
        const val PROBLEM_JSON = "application/problem+json"
        const val MAX_DSL = 8192
    }
}
