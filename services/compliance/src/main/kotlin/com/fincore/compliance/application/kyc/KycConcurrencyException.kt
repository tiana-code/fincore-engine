// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

/** Raised when a KYC initiation could not settle against a concurrent caller after the bounded retries. */
class KycConcurrencyException(
    cause: Throwable,
) : RuntimeException("Could not settle a concurrent KYC initiation after retries", cause)
