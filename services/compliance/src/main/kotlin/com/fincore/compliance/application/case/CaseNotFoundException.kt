// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.core.ComplianceCaseId

class CaseNotFoundException(
    id: ComplianceCaseId,
) : RuntimeException("Compliance case not found: $id")
