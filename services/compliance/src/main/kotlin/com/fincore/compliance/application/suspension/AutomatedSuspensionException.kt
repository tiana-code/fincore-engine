// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.suspension

/** Signals a technical or transient failure performing an automated suspension, distinct from a business outcome. */
class AutomatedSuspensionException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
