// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fincore.payments.webhook")
data class PaymentWebhookProperties(
    val hmacSecret: String = "",
)
