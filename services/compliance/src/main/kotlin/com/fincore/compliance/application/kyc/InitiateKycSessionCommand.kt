// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

// subjectReference is an opaque token, never raw PII; idempotencyKey dedupes a repeated initiation.
data class InitiateKycSessionCommand(
    val idempotencyKey: String,
    val subjectReference: String,
)
