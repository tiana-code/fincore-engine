// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

/** Signals a technical or transient failure running a KYC check, distinct from a business decision. */
class KycProviderException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
