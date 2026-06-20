// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.sdk.model.ComplianceCase
import com.fincore.sdk.model.KycSession
import java.net.http.HttpClient

class ComplianceClient(
    baseUrl: String,
    token: String? = null,
    httpClient: HttpClient = HttpClient.newHttpClient(),
    mapper: ObjectMapper = JsonHttp.defaultMapper(),
) {
    private val http = JsonHttp(baseUrl, token, httpClient, mapper)

    fun listCases(status: String): List<ComplianceCase> = http.get("/v1/compliance/cases?status=${http.encode(status)}", CASE_LIST)

    fun getCase(id: String): ComplianceCase = http.get("/v1/compliance/cases/${http.encode(id)}", ComplianceCase::class.java)

    fun getKycSession(id: String): KycSession = http.get("/v1/kyc/sessions/${http.encode(id)}", KycSession::class.java)

    private companion object {
        val CASE_LIST = object : TypeReference<List<ComplianceCase>>() {}
    }
}
