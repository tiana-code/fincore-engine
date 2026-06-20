// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.sdk.model.Page
import com.fincore.sdk.model.Payment
import java.net.http.HttpClient

class PaymentsClient(
    baseUrl: String,
    token: String? = null,
    httpClient: HttpClient = HttpClient.newHttpClient(),
    mapper: ObjectMapper = JsonHttp.defaultMapper(),
) {
    private val http = JsonHttp(baseUrl, token, httpClient, mapper)

    fun listPayments(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
    ): Page<Payment> = http.get("/v1/payments?page=$page&size=$size", PAYMENT_PAGE)

    fun getPayment(id: String): Payment = http.get("/v1/payments/${http.encode(id)}", Payment::class.java)

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
        val PAYMENT_PAGE = object : TypeReference<Page<Payment>>() {}
    }
}
