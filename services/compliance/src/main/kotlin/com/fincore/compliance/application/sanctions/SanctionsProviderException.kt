// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.sanctions

/** Signals a technical or transient failure running a sanctions screening, distinct from a business decision. */
class SanctionsProviderException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
