// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

class CurrencyMismatchException(
    left: Currency,
    right: Currency,
) : IllegalArgumentException("Currency mismatch: cannot mix ${left.code} and ${right.code}")
