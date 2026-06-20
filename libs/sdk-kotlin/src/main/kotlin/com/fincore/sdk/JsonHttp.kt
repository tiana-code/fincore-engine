// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

internal class JsonHttp(
    baseUrl: String,
    private val token: String?,
    private val httpClient: HttpClient,
    private val mapper: ObjectMapper,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    fun <T> get(
        path: String,
        type: Class<T>,
    ): T = mapper.readValue(getRaw(path), type)

    fun <T> get(
        path: String,
        type: TypeReference<T>,
    ): T = mapper.readValue(getRaw(path), type)

    fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun getRaw(path: String): String {
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

    companion object {
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299

        fun defaultMapper(): ObjectMapper =
            jacksonObjectMapper().apply { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
    }
}
