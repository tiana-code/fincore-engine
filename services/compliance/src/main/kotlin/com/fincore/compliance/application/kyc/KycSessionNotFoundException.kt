// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.core.KycSessionId

class KycSessionNotFoundException(
    id: KycSessionId,
) : RuntimeException("KYC session not found: $id")
