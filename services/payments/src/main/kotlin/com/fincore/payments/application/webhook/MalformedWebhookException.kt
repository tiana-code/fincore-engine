// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.webhook

class MalformedWebhookException(
    cause: Throwable,
) : RuntimeException("malformed webhook payload", cause)
