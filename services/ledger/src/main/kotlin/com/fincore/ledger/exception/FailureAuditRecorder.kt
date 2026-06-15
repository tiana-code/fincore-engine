// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.exception

import com.fincore.ledger.api.error.AuditEndpointResolver
import com.fincore.ledger.api.error.ProblemType
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.security.CurrentActor
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.application.RequestHashing
import com.fincore.ledger.domain.enum.AuditResult
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FailureAuditRecorder(
    private val auditTrailWriter: AuditTrailWriter,
    private val endpointResolver: AuditEndpointResolver,
) {
    fun record(
        request: HttpServletRequest,
        problemType: ProblemType,
    ) {
        if (problemType !in FAILURE_PROBLEM_TYPES) return
        val endpoint = endpointResolver.resolve(request.method, request.requestURI) ?: return
        val actor = CurrentActor.resolveOrNull() ?: return
        val record =
            AuditRecord(
                actorId = actor,
                action = endpoint.action,
                resourceType = endpoint.resourceType,
                resourceId = endpoint.resourceId,
                requestHash = requestHash(request),
                payload = mapOf("code" to problemType.code),
            )
        try {
            auditTrailWriter.recordOutcome(record, AuditResult.FAILURE)
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            log.warn("audit FAILURE write failed for {} {}", request.method, request.requestURI, ex)
        }
    }

    private fun requestHash(request: HttpServletRequest): String? =
        (request.getAttribute(IdempotencyAttributes.BODY) as? String)?.let { RequestHashing.sha256Hex(it) }

    private companion object {
        val log = LoggerFactory.getLogger(FailureAuditRecorder::class.java)
        val FAILURE_PROBLEM_TYPES =
            setOf(
                ProblemType.DOUBLE_ENTRY_VIOLATION,
                ProblemType.CURRENCY_CONSISTENCY_VIOLATION,
                ProblemType.TRANSACTION_ALREADY_REVERSED,
                ProblemType.CONCURRENCY_CONFLICT,
                ProblemType.DUPLICATE_TRANSACTION,
                ProblemType.DOMAIN_RULE_VIOLATION,
                ProblemType.TRANSACTION_NOT_FOUND,
                ProblemType.IDEMPOTENCY_CONFLICT,
            )
    }
}
