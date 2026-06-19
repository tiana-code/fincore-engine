// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.sanctions

/**
 * Plug-in port for screening a subject against a sanctions source.
 *
 * Real screening adapters and lists are supplied out of tree and are NOT part of this open-source service; the only
 * in-tree implementation is a deterministic sandbox. The contract is generic and encodes no real list, entry, format,
 * or business threshold.
 *
 * Implementations perform no persistence; the caller decides what to do with the result, outside any transaction.
 * A business outcome is returned as a [SanctionsScreeningResult]; a technical or transient failure is thrown as a
 * [SanctionsProviderException].
 */
interface SanctionsProvider {
    fun screen(request: SanctionsScreeningRequest): SanctionsScreeningResult
}
