// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.dto.request

import com.fincore.payments.application.webhook.WebhookOutcome

data class WebhookRequest(
    val deliveryId: String,
    val providerReference: String,
    val outcome: WebhookOutcome,
)
