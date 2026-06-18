// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.bank

/**
 * Plug-in port for submitting a payment to an external bank.
 *
 * Real bank adapters are provided out of tree and are NOT part of this open-source service; the only in-tree
 * implementation is a deterministic sandbox. The contract is generic and encodes no real-bank protocol, field,
 * or business value.
 *
 * Implementations perform no persistence; the caller decides what to do with the result, outside any transaction.
 * A bank decision is returned as a [BankSubmissionResult]; a technical or transient failure is thrown as a
 * [BankProviderException].
 */
interface BankProvider {
    fun submit(request: BankPaymentRequest): BankSubmissionResult
}
