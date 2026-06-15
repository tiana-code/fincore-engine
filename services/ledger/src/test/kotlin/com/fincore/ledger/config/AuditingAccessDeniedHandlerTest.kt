// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.ledger.api.error.AuditEndpointResolver
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.domain.enum.AuditResult
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class AuditingAccessDeniedHandlerTest {
    private val auditTrailWriter = mockk<AuditTrailWriter>()
    private val objectMapper = jacksonObjectMapper()
    private val handler = AuditingAccessDeniedHandler(auditTrailWriter, AuditEndpointResolver(), objectMapper)

    @BeforeEach
    fun authenticate() {
        val authentication = TestingAuthenticationToken(ACTOR, "credentials")
        authentication.isAuthenticated = true
        SecurityContextHolder.getContext().authentication = authentication
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        clearAllMocks()
    }

    @Test
    fun `should record a DENIED outcome with null hash and write a 403 problem`() {
        val recordSlot = slot<AuditRecord>()
        val resultSlot = slot<AuditResult>()
        every { auditTrailWriter.recordOutcome(capture(recordSlot), capture(resultSlot)) } just Runs
        val response = MockHttpServletResponse()

        handler.handle(MockHttpServletRequest("POST", "/v1/accounts"), response, AccessDeniedException("denied"))

        resultSlot.captured shouldBe AuditResult.DENIED
        recordSlot.captured.actorId shouldBe ACTOR
        recordSlot.captured.requestHash shouldBe null
        recordSlot.captured.payload shouldBe mapOf("code" to "ACCESS_DENIED")
        response.status shouldBe 403
        response.contentType shouldBe "application/problem+json"
        response.contentAsString shouldContain "ACCESS_DENIED"
    }

    @Test
    fun `should still write the 403 when the audit write fails`() {
        every { auditTrailWriter.recordOutcome(any(), any()) } throws RuntimeException("db down")
        val response = MockHttpServletResponse()

        shouldNotThrowAny {
            handler.handle(MockHttpServletRequest("POST", "/v1/accounts"), response, AccessDeniedException("denied"))
        }

        response.status shouldBe 403
    }

    @Test
    fun `should write a 403 without an audit row for a non-write endpoint`() {
        val response = MockHttpServletResponse()

        handler.handle(MockHttpServletRequest("GET", "/v1/accounts/acc_1"), response, AccessDeniedException("denied"))

        response.status shouldBe 403
        verify(exactly = 0) { auditTrailWriter.recordOutcome(any(), any()) }
    }

    private companion object {
        const val ACTOR = "auth0|test-actor"
    }
}
