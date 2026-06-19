// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.sdk.model.Account
import com.fincore.sdk.model.Page
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class FincoreClient(
    baseUrl: String,
    private val token: String? = null,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = defaultMapper(),
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun listAccounts(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
    ): Page<Account> = mapper.readValue(get("/v1/accounts?page=$page&size=$size"), ACCOUNT_PAGE)

    fun getAccount(id: String): Account =
        mapper.readValue(get("/v1/accounts/${URLEncoder.encode(id, StandardCharsets.UTF_8)}"), Account::class.java)

    private fun get(path: String): String {
        val builder =
            HttpRequest
                .newBuilder()
                .uri(URI.create("$baseUrl$path"))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .GET()
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        val response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in HTTP_OK_MIN..HTTP_OK_MAX) {
            throw FincoreException(response.statusCode(), "GET $path failed with HTTP ${response.statusCode()}")
        }
        return response.body()
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299
        val ACCOUNT_PAGE = object : TypeReference<Page<Account>>() {}

        fun defaultMapper(): ObjectMapper =
            jacksonObjectMapper().apply { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
    }
}
