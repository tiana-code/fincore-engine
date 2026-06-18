// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.exception

import com.fincore.payments.api.error.ProblemType
import com.fincore.payments.application.PaymentConcurrencyException
import com.fincore.payments.domain.exception.PaymentDomainException
import com.fincore.payments.domain.exception.PaymentNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException::class)
    fun handleNotFound(
        ex: PaymentNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.PAYMENT_NOT_FOUND, ex.message, request)

    @ExceptionHandler(PaymentDomainException::class)
    fun handleDomain(
        ex: PaymentDomainException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.PAYMENT_CONFLICT, ex.message, request)

    @ExceptionHandler(PaymentConcurrencyException::class)
    fun handleConcurrency(
        ex: PaymentConcurrencyException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> = retryable(ProblemType.CONCURRENCY_CONFLICT, ex.message, request)

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

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INVALID_REQUEST, ex.message, request)

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
