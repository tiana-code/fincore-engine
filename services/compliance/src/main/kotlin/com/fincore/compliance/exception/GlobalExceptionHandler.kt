// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.exception

import com.fincore.compliance.api.error.ProblemType
import com.fincore.compliance.application.case.CaseNotFoundException
import com.fincore.compliance.application.kyc.KycConcurrencyException
import com.fincore.compliance.application.kyc.KycSessionNotFoundException
import com.fincore.compliance.domain.ComplianceDomainException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(KycSessionNotFoundException::class)
    fun handleNotFound(
        ex: KycSessionNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.KYC_NOT_FOUND, ex.message, request)

    @ExceptionHandler(CaseNotFoundException::class)
    fun handleCaseNotFound(
        ex: CaseNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.CASE_NOT_FOUND, ex.message, request)

    @ExceptionHandler(ComplianceDomainException::class)
    fun handleDomain(
        ex: ComplianceDomainException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.COMPLIANCE_CONFLICT, ex.message, request)

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(request: HttpServletRequest): ProblemDetail =
        problem(ProblemType.COMPLIANCE_CONFLICT, "Concurrent update conflict", request)

    @ExceptionHandler(KycConcurrencyException::class)
    fun handleConcurrency(
        ex: KycConcurrencyException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> = retryable(ProblemType.CONCURRENCY_CONFLICT, ex.message, request)

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INVALID_REQUEST, ex.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ProblemDetail =
        problem(ProblemType.VALIDATION_FAILED, "Request validation failed", request).apply {
            setProperty("errors", ex.bindingResult.fieldErrors.map(::toFieldError))
        }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(request: HttpServletRequest): ProblemDetail =
        problem(ProblemType.MALFORMED_REQUEST, "Request body is missing or malformed", request)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INVALID_REQUEST, "Invalid value for parameter '${ex.name}'", request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INVALID_REQUEST, ex.message, request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(request: HttpServletRequest): ProblemDetail =
        problem(ProblemType.INTERNAL_ERROR, "An unexpected error occurred", request)

    private fun toFieldError(error: FieldError): Map<String, String> =
        mapOf("field" to error.field, "message" to (error.defaultMessage ?: "invalid"))

    private fun retryable(
        type: ProblemType,
        detail: String?,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity
            .status(type.status)
            .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
            .body(problem(type, detail, request))

    private fun problem(
        type: ProblemType,
        detail: String?,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(type.status, detail ?: type.title).apply {
            this.title = type.title
            this.type = type.type
            this.instance = URI.create(request.requestURI)
            setProperty("code", type.code)
        }

    private companion object {
        const val RETRY_AFTER_SECONDS = "1"
    }
}
