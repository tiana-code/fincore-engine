// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.ledger.api.error.AuditEndpointResolver
import com.fincore.ledger.api.error.ProblemType
import com.fincore.ledger.api.security.CurrentActor
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.domain.enum.AuditResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.net.URI

@Component
class AuditingAccessDeniedHandler(
    private val auditTrailWriter: AuditTrailWriter,
    private val endpointResolver: AuditEndpointResolver,
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        recordDenied(request)
        writeForbidden(request, response)
    }

    private fun recordDenied(request: HttpServletRequest) {
        val endpoint = endpointResolver.resolve(request.method, request.requestURI) ?: return
        val actor = CurrentActor.resolveOrNull() ?: return
        val record =
            AuditRecord(
                actorId = actor,
                action = endpoint.action,
                resourceType = endpoint.resourceType,
                resourceId = endpoint.resourceId,
                requestHash = null,
                payload = mapOf("code" to ProblemType.ACCESS_DENIED.code),
            )
        try {
            auditTrailWriter.recordOutcome(record, AuditResult.DENIED)
        } catch (
            @Suppress("TooGenericExceptionCaught") ex: RuntimeException,
        ) {
            log.warn("audit DENIED write failed for {} {}", request.method, request.requestURI, ex)
        }
    }

    private fun writeForbidden(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val type = ProblemType.ACCESS_DENIED
        val problem = ProblemDetail.forStatusAndDetail(type.status, type.title)
        problem.title = type.title
        problem.type = type.type
        problem.instance = URI.create(request.requestURI)
        problem.setProperty("code", type.code)
        response.status = type.status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.writer, problem)
    }

    private companion object {
        val log = LoggerFactory.getLogger(AuditingAccessDeniedHandler::class.java)
    }
}
