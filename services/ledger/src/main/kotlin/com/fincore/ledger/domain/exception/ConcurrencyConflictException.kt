// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.exception

class ConcurrencyConflictException(
    cause: Throwable,
) : RuntimeException("Posting failed after retries due to concurrent balance updates", cause)
