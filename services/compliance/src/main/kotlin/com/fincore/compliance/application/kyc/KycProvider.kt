// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

/**
 * Plug-in port for submitting a KYC check to an external provider.
 *
 * Real provider adapters are supplied out of tree and are NOT part of this open-source service; the only in-tree
 * implementation is a deterministic sandbox. The contract is generic and encodes no real-provider protocol, field,
 * or result code.
 *
 * Implementations perform no persistence; the caller decides what to do with the result, outside any transaction.
 * A business outcome is returned as a [KycCheckResult]; a technical or transient failure is thrown as a
 * [KycProviderException].
 */
interface KycProvider {
    fun check(request: KycCheckRequest): KycCheckResult
}
