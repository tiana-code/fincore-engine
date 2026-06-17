// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api.error

import org.springframework.http.HttpStatus
import java.net.URI

enum class ProblemType(
    val slug: String,
    val status: HttpStatus,
    val code: String,
    val title: String,
) {
    RULE_NOT_FOUND("rule-not-found", HttpStatus.NOT_FOUND, "RULE_NOT_FOUND", "rule not found"),
    DUPLICATE_RULE_KEY("duplicate-rule-key", HttpStatus.CONFLICT, "DUPLICATE_RULE_KEY", "rule key already exists"),
    INVALID_DSL("invalid-dsl", HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DSL", "invalid decision rule document"),
    DSL_TOO_LARGE("dsl-too-large", HttpStatus.UNPROCESSABLE_ENTITY, "DSL_TOO_LARGE", "decision rule document too large"),
    VERSION_CONFLICT("version-conflict", HttpStatus.CONFLICT, "VERSION_CONFLICT", "concurrent version publish conflict"),
    CONCURRENCY_CONFLICT("concurrency-conflict", HttpStatus.SERVICE_UNAVAILABLE, "CONCURRENCY_CONFLICT", "concurrency conflict, retry"),
    RULE_NOT_ACTIVE("rule-not-active", HttpStatus.CONFLICT, "RULE_NOT_ACTIVE", "rule has no active version"),
    EVALUATION_TIMEOUT("evaluation-timeout", HttpStatus.SERVICE_UNAVAILABLE, "EVALUATION_TIMEOUT", "evaluation timed out, retry"),
    INPUT_NOT_MAPPABLE("input-not-mappable", HttpStatus.UNPROCESSABLE_ENTITY, "INPUT_NOT_MAPPABLE", "input could not be mapped"),
    INPUT_TOO_LARGE("input-too-large", HttpStatus.UNPROCESSABLE_ENTITY, "INPUT_TOO_LARGE", "input exceeds the allowed size"),
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
