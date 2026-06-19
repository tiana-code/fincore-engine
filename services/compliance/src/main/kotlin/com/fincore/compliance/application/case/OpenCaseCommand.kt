// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

/** Command to open a compliance case. [reference] is a generic, opaque case reference (validated by the domain). */
data class OpenCaseCommand(
    val reference: String,
)
