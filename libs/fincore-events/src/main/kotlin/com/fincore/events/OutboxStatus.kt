// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

enum class OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    PERMANENTLY_FAILED,
}
