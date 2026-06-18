// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookSignatureVerifierTest {
    private val secret = "payments-test-key"
    private val verifier = WebhookSignatureVerifier(PaymentWebhookProperties(hmacSecret = secret))
    private val payload = """{"deliveryId":"d-1","providerReference":"prov-1","outcome":"SETTLED"}"""

    @Test
    fun `should accept a signature produced with the configured secret`() {
        verifier.verify(payload, sign(payload, secret)) shouldBe true
    }

    @Test
    fun `should accept an upper-case signature`() {
        verifier.verify(payload, sign(payload, secret).uppercase()) shouldBe true
    }

    @Test
    fun `should accept a signature carrying surrounding whitespace`() {
        verifier.verify(payload, " ${sign(payload, secret)}\n") shouldBe true
    }

    @Test
    fun `should reject a tampered signature`() {
        verifier.verify(payload, sign("other", secret)) shouldBe false
    }

    @Test
    fun `should reject when the secret is not configured`() {
        WebhookSignatureVerifier(PaymentWebhookProperties()).verify(payload, sign(payload, secret)) shouldBe false
    }

    private fun sign(
        body: String,
        key: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(body.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
