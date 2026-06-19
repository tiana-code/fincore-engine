// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.observability

object PiiMasker {
    const val REDACTION = "[REDACTED]"

    private val BEARER = Regex("(?i)Bearer\\s{1,4}[A-Za-z0-9._~+/=-]{8,512}")
    private val EMAIL = Regex("[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,24}")

    // 13+ contiguous digits (PAN/account/long-id); the lookarounds take the maximal run so a 20+ digit
    // sequence is masked whole, while a UUID's fixed 12-digit node group stays below the threshold.
    private val LONG_DIGITS = Regex("(?<!\\d)\\d{13,}(?!\\d)")

    fun mask(input: String): String =
        input
            .replace(BEARER, REDACTION)
            .replace(EMAIL, REDACTION)
            .replace(LONG_DIGITS, REDACTION)
}
