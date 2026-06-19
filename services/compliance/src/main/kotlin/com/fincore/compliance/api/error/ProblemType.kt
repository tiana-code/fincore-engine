// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api.error

import org.springframework.http.HttpStatus
import java.net.URI

enum class ProblemType(
    val slug: String,
    val status: HttpStatus,
    val code: String,
    val title: String,
) {
    KYC_NOT_FOUND("kyc-session-not-found", HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "kyc session not found"),
    CASE_NOT_FOUND("compliance-case-not-found", HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", "compliance case not found"),
    COMPLIANCE_CONFLICT(
        "compliance-conflict",
        HttpStatus.CONFLICT,
        "COMPLIANCE_CONFLICT",
        "illegal compliance state transition",
    ),
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
