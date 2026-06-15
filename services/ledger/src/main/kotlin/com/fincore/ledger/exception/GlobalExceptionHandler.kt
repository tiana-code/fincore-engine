// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.exception

import com.fincore.ledger.api.error.ProblemType
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException::class)
    fun handleAccountNotFound(
        ex: AccountNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.ACCOUNT_NOT_FOUND, ex.message, request)

    @ExceptionHandler(TransactionNotFoundException::class)
    fun handleTransactionNotFound(
        ex: TransactionNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.TRANSACTION_NOT_FOUND, ex.message, request)

    @ExceptionHandler(DuplicateTransactionException::class)
    fun handleDuplicateTransaction(
        ex: DuplicateTransactionException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.DUPLICATE_TRANSACTION, ex.message, request)

    @ExceptionHandler(TransactionAlreadyReversedException::class)
    fun handleTransactionAlreadyReversed(
        ex: TransactionAlreadyReversedException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.TRANSACTION_ALREADY_REVERSED, ex.message, request)

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(
        ex: IdempotencyConflictException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.IDEMPOTENCY_CONFLICT, ex.message, request)

    @ExceptionHandler(CurrencyConsistencyViolationException::class)
    fun handleCurrencyConsistency(
        ex: CurrencyConsistencyViolationException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.CURRENCY_CONSISTENCY_VIOLATION, ex.message, request)

    @ExceptionHandler(DoubleEntryViolationException::class)
    fun handleDoubleEntry(
        ex: DoubleEntryViolationException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.DOUBLE_ENTRY_VIOLATION, ex.message, request)

    @ExceptionHandler(DomainException::class)
    fun handleDomain(
        ex: DomainException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(ProblemType.DOMAIN_RULE_VIOLATION, ex.message, request)

    @ExceptionHandler(ConcurrencyConflictException::class)
    fun handleConcurrencyConflict(
        ex: ConcurrencyConflictException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val body = problem(ProblemType.CONCURRENCY_CONFLICT, ex.message, request)
        return ResponseEntity
            .status(ProblemType.CONCURRENCY_CONFLICT.status)
            .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
            .body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ProblemDetail {
        val detail = problem(ProblemType.VALIDATION_FAILED, "Request validation failed", request)
        detail.setProperty("errors", ex.bindingResult.fieldErrors.map(::toFieldError))
        return detail
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
            "code" to constraintCode(error),
            "message" to (error.defaultMessage ?: "invalid"),
        )

    private fun constraintCode(error: FieldError): String {
        val constraint = error.codes?.lastOrNull()?.substringAfterLast('.') ?: return "INVALID"
        return constraint.replace(CAMEL_BOUNDARY, "_").uppercase()
    }

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
        val CAMEL_BOUNDARY = Regex("(?<=.)(?=\\p{Lu})")
    }
}
