// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { HttpTransport, type FincoreClientOptions } from './http.js'
import type { CaseStatus, ComplianceCase, KycSession } from './types.js'

export class ComplianceClient {
    private readonly http: HttpTransport

    constructor(options: FincoreClientOptions) {
        this.http = new HttpTransport(options)
    }

    listCases(status: CaseStatus): Promise<ComplianceCase[]> {
        return this.http.get<ComplianceCase[]>(`/v1/compliance/cases?status=${encodeURIComponent(status)}`)
    }

    getCase(id: string): Promise<ComplianceCase> {
        return this.http.get<ComplianceCase>(`/v1/compliance/cases/${encodeURIComponent(id)}`)
    }

    getKycSession(id: string): Promise<KycSession> {
        return this.http.get<KycSession>(`/v1/kyc/sessions/${encodeURIComponent(id)}`)
    }
}
