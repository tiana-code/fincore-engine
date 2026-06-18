// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.config

import com.fincore.payments.application.screening.PaymentScreeningProperties
import com.fincore.payments.application.webhook.PaymentWebhookProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PaymentScreeningProperties::class, PaymentWebhookProperties::class)
class PaymentsConfig
