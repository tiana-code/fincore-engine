// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

data class PaymentWebhookNotification(
    val deliveryId: String,
    val providerReference: String,
    val outcome: WebhookOutcome,
)
