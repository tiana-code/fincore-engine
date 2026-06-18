// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

class PaymentConcurrencyException(
    cause: Throwable,
) : RuntimeException("Could not settle a concurrent payment initiation after retries", cause)
