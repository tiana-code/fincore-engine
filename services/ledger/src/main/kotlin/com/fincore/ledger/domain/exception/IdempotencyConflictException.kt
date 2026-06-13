// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.exception

class IdempotencyConflictException : DomainException("Idempotency key reused with a different request payload")
