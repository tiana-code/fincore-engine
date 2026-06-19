// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

private const val NOT_FOUND = 404

class FincoreClientTest :
    StringSpec({

        val accountJson =
            """{"id":"acc_1","name":"Cash","type":"ASSET","currency":"USD","status":"ACTIVE","futureField":"ignored"}"""
        val pageJson = """{"items":[$accountJson],"page":0,"size":20,"totalElements":1,"totalPages":1}"""

        lateinit var server: HttpServer
        var seenAuthorization: String? = null

        fun HttpExchange.reply(
            status: Int,
            body: String,
        ) {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            sendResponseHeaders(status, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
        }

        beforeSpec {
            server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/v1/accounts") { exchange ->
                seenAuthorization = exchange.requestHeaders.getFirst("Authorization")
                when (exchange.requestURI.path) {
                    "/v1/accounts" -> exchange.reply(200, pageJson)
                    "/v1/accounts/acc_1" -> exchange.reply(200, accountJson)
                    else -> exchange.reply(NOT_FOUND, """{"detail":"not found"}""")
                }
            }
            server.start()
        }

        afterSpec { server.stop(0) }

        "lists accounts and decodes the page" {
            val client = FincoreClient("http://127.0.0.1:${server.address.port}", token = "test-token")
            val page = client.listAccounts()
            page.totalElements shouldBe 1
            page.items.single().id shouldBe "acc_1"
            page.items.single().type shouldBe "ASSET"
        }

        "sends the bearer token" {
            FincoreClient("http://127.0.0.1:${server.address.port}", token = "test-token").listAccounts()
            seenAuthorization shouldBe "Bearer test-token"
        }

        "gets a single account" {
            val client = FincoreClient("http://127.0.0.1:${server.address.port}")
            client.getAccount("acc_1").name shouldBe "Cash"
        }

        "raises FincoreException with the status on a non-2xx response" {
            val client = FincoreClient("http://127.0.0.1:${server.address.port}")
            val ex = shouldThrow<FincoreException> { client.getAccount("missing") }
            ex.status shouldBe NOT_FOUND
        }
    })
