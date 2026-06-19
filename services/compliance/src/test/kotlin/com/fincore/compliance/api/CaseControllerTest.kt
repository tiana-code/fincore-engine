// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api

import com.fincore.compliance.api.mapper.CaseApiMapper
import com.fincore.compliance.application.case.CaseNotFoundException
import com.fincore.compliance.application.case.ComplianceCaseService
import com.fincore.compliance.config.SecurityConfig
import com.fincore.compliance.domain.ComplianceCase
import com.fincore.compliance.domain.ComplianceDomainException
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.exception.GlobalExceptionHandler
import com.fincore.core.ComplianceCaseId
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CaseController::class)
@Import(SecurityConfig::class, CaseApiMapper::class, GlobalExceptionHandler::class, CaseControllerTest.Mocks::class)
class CaseControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val caseService: ComplianceCaseService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun caseService(): ComplianceCaseService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(caseService)
    }

    @Test
    fun `should open a case and return 201 with write scope`() {
        every { caseService.open(any()) } returns case(CaseStatus.OPEN)

        mockMvc
            .perform(
                post("/v1/compliance/cases")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    fun `should return 401 when opening without a token`() {
        mockMvc
            .perform(post("/v1/compliance/cases").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 403 when opening with only the read scope`() {
        mockMvc
            .perform(
                post("/v1/compliance/cases")
                    .with(jwt().authorities(SimpleGrantedAuthority(READ)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should get a case with read scope`() {
        val case = case(CaseStatus.OPEN)
        every { caseService.get(any()) } returns case

        mockMvc
            .perform(get("/v1/compliance/cases/${case.id}").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(case.id.toString()))
    }

    @Test
    fun `should return 400 for an unparseable case id`() {
        mockMvc
            .perform(get("/v1/compliance/cases/not-a-ulid").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for an unknown status filter`() {
        mockMvc
            .perform(get("/v1/compliance/cases?status=BOGUS").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 404 when getting an unknown case`() {
        every { caseService.get(any()) } throws CaseNotFoundException(ComplianceCaseId.generate())

        mockMvc
            .perform(get("/v1/compliance/cases/${ComplianceCaseId.generate()}").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 409 when a transition is illegal`() {
        every { caseService.claim(any()) } throws ComplianceDomainException("illegal transition")

        mockMvc
            .perform(
                post("/v1/compliance/cases/${ComplianceCaseId.generate()}/claim").with(jwt().authorities(SimpleGrantedAuthority(WRITE))),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `should list cases by status with read scope`() {
        every { caseService.list(CaseStatus.OPEN) } returns listOf(case(CaseStatus.OPEN))

        mockMvc
            .perform(get("/v1/compliance/cases?status=OPEN").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("OPEN"))
    }

    private fun case(status: CaseStatus): ComplianceCase = ComplianceCase(ComplianceCaseId.generate(), "case-ref-1", status)

    private companion object {
        const val READ = "SCOPE_compliance:read"
        const val WRITE = "SCOPE_compliance:write"
        const val VALID_BODY = """{"reference":"case-ref-1"}"""
    }
}
