// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.domain.exception

import com.fincore.core.PaymentId

class PaymentNotFoundException(
    id: PaymentId,
) : RuntimeException("Payment not found: $id")
