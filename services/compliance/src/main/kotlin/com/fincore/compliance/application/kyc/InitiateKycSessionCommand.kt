// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

/** Command to start a KYC session. [subjectReference] is an opaque token (validated by the KycSession domain). */
data class InitiateKycSessionCommand(
    val subjectReference: String,
)
