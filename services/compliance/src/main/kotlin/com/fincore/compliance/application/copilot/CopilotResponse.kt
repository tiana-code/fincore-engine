// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.copilot

/**
 * Advisory output of an [AmlCopilot] assist call. [summary] and [recommendations] are non-authoritative guidance for
 * the human reviewer and carry no PII.
 */
data class CopilotResponse(
    val summary: String,
    val recommendations: List<String> = emptyList(),
)
