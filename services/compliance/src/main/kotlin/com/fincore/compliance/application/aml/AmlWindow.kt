// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.aml

/**
 * Aggregation windows. PER_TRANSACTION = the single most recent observation by occurrence time; MONTH = observations
 * within the last 30 days; LIFETIME = all observations.
 */
enum class AmlWindow {
    PER_TRANSACTION,
    MONTH,
    LIFETIME,
}
