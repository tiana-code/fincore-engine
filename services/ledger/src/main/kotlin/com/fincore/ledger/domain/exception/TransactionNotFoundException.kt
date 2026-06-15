// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.exception

import com.fincore.core.TransactionId

class TransactionNotFoundException(
    id: TransactionId,
) : DomainException("Transaction not found: $id")
