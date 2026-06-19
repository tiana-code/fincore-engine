// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.copilot

/** Signals a technical or transient failure producing copilot assistance, distinct from advisory output. */
class AmlCopilotException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
