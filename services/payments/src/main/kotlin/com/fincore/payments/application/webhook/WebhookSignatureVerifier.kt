// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies an inbound webhook's HMAC-SHA256 signature against the configured shared secret. Fail-closed: a blank
 * secret or a mismatch returns false. The compare is constant-time over case-normalized hex.
 */
@Component
class WebhookSignatureVerifier(
    private val properties: PaymentWebhookProperties,
) {
    fun verify(
        payload: String,
        signatureHex: String,
    ): Boolean {
        if (properties.hmacSecret.isBlank()) return false
        val expected = hmacHex(payload).toByteArray(Charsets.UTF_8)
        val provided = signatureHex.trim().lowercase().toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, provided)
    }

    private fun hmacHex(payload: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(properties.hmacSecret.toByteArray(Charsets.UTF_8), ALGORITHM))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}
