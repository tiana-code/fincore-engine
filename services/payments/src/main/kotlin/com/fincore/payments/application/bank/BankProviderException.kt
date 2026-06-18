// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.bank

/** Signals a technical or transient failure submitting to a bank, distinct from a business rejection. */
class BankProviderException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
