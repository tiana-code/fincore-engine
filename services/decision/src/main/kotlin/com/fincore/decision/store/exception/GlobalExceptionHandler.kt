// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.exception

import com.fincore.decision.store.api.error.ProblemType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuleNotFoundException::class)
    fun handleRuleNotFound(
        ex: RuleNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.RULE_NOT_FOUND, ex.message, request)

    @ExceptionHandler(DuplicateRuleKeyException::class)
    fun handleDuplicateRuleKey(
        ex: DuplicateRuleKeyException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.DUPLICATE_RULE_KEY, ex.message, request)

    @ExceptionHandler(InvalidRuleDslException::class)
    fun handleInvalidDsl(
        ex: InvalidRuleDslException,
        request: HttpServletRequest,
    ): ProblemDetail =
        problem(ProblemType.INVALID_DSL, ex.message, request).apply {
            setProperty("dslCode", ex.dslCode.name)
        }

    @ExceptionHandler(DslTooLargeException::class)
    fun handleDslTooLarge(
        ex: DslTooLargeException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.DSL_TOO_LARGE, ex.message, request)

    @ExceptionHandler(VersionConflictException::class)
    fun handleVersionConflict(
        ex: VersionConflictException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.VERSION_CONFLICT, ex.message, request)

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        retryable(ProblemType.CONCURRENCY_CONFLICT, "concurrent update, retry", request)

    @ExceptionHandler(RuleNotActiveException::class)
    fun handleRuleNotActive(
        ex: RuleNotActiveException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.RULE_NOT_ACTIVE, ex.message, request)

    @ExceptionHandler(EvaluationTimeoutException::class)
    fun handleEvaluationTimeout(
        ex: EvaluationTimeoutException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> = retryable(ProblemType.EVALUATION_TIMEOUT, ex.message, request)

    @ExceptionHandler(InputNotMappableException::class)
    fun handleInputNotMappable(
        ex: InputNotMappableException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INPUT_NOT_MAPPABLE, ex.message, request)

    @ExceptionHandler(InputTooLargeException::class)
    fun handleInputTooLarge(
        ex: InputTooLargeException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INPUT_TOO_LARGE, ex.message, request)

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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.INVALID_REQUEST, ex.message, request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(request: HttpServletRequest): ProblemDetail =
        problem(ProblemType.INTERNAL_ERROR, "An unexpected error occurred", request)

    private fun toFieldError(error: FieldError): Map<String, String> =
        mapOf(
            "field" to error.field,
            "message" to (error.defaultMessage ?: "invalid"),
        )

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
