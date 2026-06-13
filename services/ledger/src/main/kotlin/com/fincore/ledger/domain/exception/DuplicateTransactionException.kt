// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.exception

class DuplicateTransactionException(
    reference: String,
    cause: Throwable? = null,
) : DomainException("Transaction reference already posted: $reference", cause)
