// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.error

import org.springframework.http.HttpStatus
import java.net.URI

// type URI and code are stable public API: GlobalExceptionHandler and IdempotencyFilter resolve them
// from here so the contract cannot drift when a human-readable title changes.
enum class ProblemType(
    val slug: String,
    val status: HttpStatus,
    val code: String,
    val title: String,
) {
    ACCOUNT_NOT_FOUND("account-not-found", HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "account not found"),
    TRANSACTION_NOT_FOUND("transaction-not-found", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "transaction not found"),
    DUPLICATE_TRANSACTION(
        "duplicate-transaction",
        HttpStatus.CONFLICT,
        "DUPLICATE_TRANSACTION_REFERENCE",
        "duplicate transaction reference",
    ),
    TRANSACTION_ALREADY_REVERSED(
        "transaction-already-reversed",
        HttpStatus.CONFLICT,
        "TRANSACTION_ALREADY_REVERSED",
        "transaction already reversed",
    ),
    IDEMPOTENCY_CONFLICT("idempotency-conflict", HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "idempotency key conflict"),
    CURRENCY_CONSISTENCY_VIOLATION(
        "currency-consistency-violation",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "CURRENCY_CONSISTENCY_VIOLATION",
        "currency consistency violation",
    ),
    DOUBLE_ENTRY_VIOLATION("double-entry-violation", HttpStatus.UNPROCESSABLE_ENTITY, "ENTRIES_SUM_NOT_ZERO", "double-entry violation"),
    DOMAIN_RULE_VIOLATION("domain-rule-violation", HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_RULE_VIOLATION", "domain rule violation"),
    CONCURRENCY_CONFLICT("concurrency-conflict", HttpStatus.SERVICE_UNAVAILABLE, "CONCURRENCY_CONFLICT", "concurrency conflict, retry"),
    ACCESS_DENIED("access-denied", HttpStatus.FORBIDDEN, "ACCESS_DENIED", "access denied"),
    VALIDATION_FAILED("validation-failed", HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "invalid request"),
    MALFORMED_REQUEST("malformed-request", HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "invalid request"),
    INVALID_REQUEST("invalid-request", HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "invalid request"),
    INTERNAL_ERROR("internal-error", HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "internal error"),
    ;

    val type: URI get() = URI.create(BASE_URI + slug)

    companion object {
        const val BASE_URI = "https://fincore.dev/errors/"
    }
}
