// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.exception

import com.fincore.ledger.api.error.AuditEndpointResolver
import com.fincore.ledger.api.error.ProblemType
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.application.RequestHashing
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import com.fincore.ledger.domain.enum.AuditResult
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
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
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class FailureAuditRecorderTest {
    private val auditTrailWriter = mockk<AuditTrailWriter>()
    private val recorder = FailureAuditRecorder(auditTrailWriter, AuditEndpointResolver())

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
    fun `should record a FAILURE outcome with request hash and code payload for a rejected write`() {
        val recordSlot = slot<AuditRecord>()
        val resultSlot = slot<AuditResult>()
        every { auditTrailWriter.recordOutcome(capture(recordSlot), capture(resultSlot)) } just Runs
        val body = """{"reference":"ref-1","currency":"USD"}"""
        val request = MockHttpServletRequest("POST", "/v1/transactions")
        request.setAttribute(IdempotencyAttributes.BODY, body)

        recorder.record(request, ProblemType.DOUBLE_ENTRY_VIOLATION)

        resultSlot.captured shouldBe AuditResult.FAILURE
        val record = recordSlot.captured
        record.actorId shouldBe ACTOR
        record.action shouldBe AuditAction.TRANSACTION_POST
        record.resourceType shouldBe AuditResourceType.TRANSACTION
        record.resourceId shouldBe "unknown"
        record.requestHash shouldBe RequestHashing.sha256Hex(body)
        record.payload shouldBe mapOf("code" to ProblemType.DOUBLE_ENTRY_VIOLATION.code)
    }

    @Test
    fun `should record the original transaction id for a rejected reverse`() {
        val recordSlot = slot<AuditRecord>()
        every { auditTrailWriter.recordOutcome(capture(recordSlot), any()) } just Runs

        recorder.record(MockHttpServletRequest("POST", "/v1/transactions/tx_77/reverse"), ProblemType.TRANSACTION_ALREADY_REVERSED)

        recordSlot.captured.action shouldBe AuditAction.TRANSACTION_REVERSE
        recordSlot.captured.resourceId shouldBe "tx_77"
    }

    @Test
    fun `should leave request hash null when no body attribute is present`() {
        val recordSlot = slot<AuditRecord>()
        every { auditTrailWriter.recordOutcome(capture(recordSlot), any()) } just Runs

        recorder.record(MockHttpServletRequest("POST", "/v1/transactions/tx_9/reverse"), ProblemType.TRANSACTION_NOT_FOUND)

        recordSlot.captured.requestHash shouldBe null
    }

    @Test
    fun `should not record when the problem type is not failure-eligible on a write url`() {
        recorder.record(MockHttpServletRequest("POST", "/v1/transactions"), ProblemType.VALIDATION_FAILED)

        verify(exactly = 0) { auditTrailWriter.recordOutcome(any(), any()) }
    }

    @Test
    fun `should not record for a non-write endpoint`() {
        recorder.record(MockHttpServletRequest("GET", "/v1/transactions/tx_1"), ProblemType.TRANSACTION_NOT_FOUND)

        verify(exactly = 0) { auditTrailWriter.recordOutcome(any(), any()) }
    }

    @Test
    fun `should not record when there is no authenticated actor`() {
        SecurityContextHolder.clearContext()

        recorder.record(MockHttpServletRequest("POST", "/v1/transactions"), ProblemType.DOUBLE_ENTRY_VIOLATION)

        verify(exactly = 0) { auditTrailWriter.recordOutcome(any(), any()) }
    }

    @Test
    fun `should not propagate when the audit write fails`() {
        every { auditTrailWriter.recordOutcome(any(), any()) } throws RuntimeException("db down")

        shouldNotThrowAny {
            recorder.record(MockHttpServletRequest("POST", "/v1/transactions"), ProblemType.DOUBLE_ENTRY_VIOLATION)
        }
    }

    private companion object {
        const val ACTOR = "auth0|test-actor"
    }
}
