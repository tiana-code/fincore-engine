// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.suspension

/**
 * Plug-in port for an automated suspension action triggered by a caller-decided signal.
 *
 * The port encodes NO rule for what the signal is; the caller decides when to suspend. Implementations are provided
 * out of tree and are NOT part of this open-source service; the port performs no persistence. A business outcome is
 * returned as a [SuspensionResult]; a technical or transient failure is thrown as an [AutomatedSuspensionException].
 */
interface AutomatedSuspensionPort {
    fun requestSuspension(request: SuspensionRequest): SuspensionResult
}
