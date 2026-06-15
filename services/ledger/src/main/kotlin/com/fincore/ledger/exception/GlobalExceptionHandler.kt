// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.exception

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
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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
    ): ProblemDetail = problem(HttpStatus.NOT_FOUND, "account not found", ex.message, request)

    @ExceptionHandler(TransactionNotFoundException::class)
    fun handleTransactionNotFound(
        ex: TransactionNotFoundException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.NOT_FOUND, "transaction not found", ex.message, request)

    @ExceptionHandler(DuplicateTransactionException::class)
    fun handleDuplicateTransaction(
        ex: DuplicateTransactionException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.CONFLICT, "duplicate transaction reference", ex.message, request)

    @ExceptionHandler(TransactionAlreadyReversedException::class)
    fun handleTransactionAlreadyReversed(
        ex: TransactionAlreadyReversedException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.CONFLICT, "transaction already reversed", ex.message, request)

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(
        ex: IdempotencyConflictException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.CONFLICT, "idempotency key conflict", ex.message, request)

    @ExceptionHandler(CurrencyConsistencyViolationException::class)
    fun handleCurrencyConsistency(
        ex: CurrencyConsistencyViolationException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "currency consistency violation", ex.message, request)

    @ExceptionHandler(DoubleEntryViolationException::class)
    fun handleDoubleEntry(
        ex: DoubleEntryViolationException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "double-entry violation", ex.message, request)

    @ExceptionHandler(DomainException::class)
    fun handleDomain(
        ex: DomainException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.UNPROCESSABLE_ENTITY, "domain rule violation", ex.message, request)

    @ExceptionHandler(ConcurrencyConflictException::class)
    fun handleConcurrencyConflict(
        ex: ConcurrencyConflictException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val body = problem(HttpStatus.SERVICE_UNAVAILABLE, "concurrency conflict, retry", ex.message, request)
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
            .body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ProblemDetail {
        val detail = problem(HttpStatus.BAD_REQUEST, "invalid request", "Request validation failed", request)
        detail.setProperty(
            "errors",
            ex.bindingResult.fieldErrors.map { mapOf("field" to it.field, "message" to (it.defaultMessage ?: "invalid")) },
        )
        return detail
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "invalid request", "Request body is missing or malformed", request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "invalid request", ex.message, request)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(request: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal error", "An unexpected error occurred", request)

    private fun problem(
        status: HttpStatus,
        title: String,
        detail: String?,
        request: HttpServletRequest,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: title).apply {
            this.title = title
            this.type = URI.create(TYPE_PREFIX + title.replace(NON_SLUG, "-"))
            this.instance = URI.create(request.requestURI)
        }

    private companion object {
        const val TYPE_PREFIX = "urn:fincore:ledger:"
        const val RETRY_AFTER_SECONDS = "1"
        val NON_SLUG = Regex("[^a-z0-9]+")
    }
}
