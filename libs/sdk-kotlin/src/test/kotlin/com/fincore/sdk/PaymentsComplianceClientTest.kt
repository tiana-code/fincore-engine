// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

private const val NOT_FOUND = 404

class PaymentsComplianceClientTest :
    StringSpec({

        val paymentJson =
            """{"id":"pay_1","reference":"po-1","amount":125.50,"currency":"USD","status":"SETTLED","futureField":"ignored"}"""
        val paymentPageJson = """{"items":[$paymentJson],"page":0,"size":20,"totalElements":1,"totalPages":1}"""
        val caseJson = """{"id":"case_1","reference":"c-1","status":"OPEN"}"""
        val caseListJson = """[$caseJson]"""
        val kycJson = """{"id":"kyc_1","subjectReference":"subj-1","status":"APPROVED"}"""

        lateinit var server: HttpServer
        var seenAuthorization: String? = null
        var seenCaseQuery: String? = null

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
            server.createContext("/v1/payments") { exchange ->
                seenAuthorization = exchange.requestHeaders.getFirst("Authorization")
                when (exchange.requestURI.path) {
                    "/v1/payments" -> exchange.reply(200, paymentPageJson)
                    "/v1/payments/pay_1" -> exchange.reply(200, paymentJson)
                    else -> exchange.reply(NOT_FOUND, """{"detail":"not found"}""")
                }
            }
            server.createContext("/v1/compliance/cases") { exchange ->
                when (exchange.requestURI.path) {
                    "/v1/compliance/cases" -> {
                        seenCaseQuery = exchange.requestURI.query
                        exchange.reply(200, caseListJson)
                    }
                    "/v1/compliance/cases/case_1" -> exchange.reply(200, caseJson)
                    else -> exchange.reply(NOT_FOUND, """{"detail":"not found"}""")
                }
            }
            server.createContext("/v1/kyc/sessions") { exchange ->
                when (exchange.requestURI.path) {
                    "/v1/kyc/sessions/kyc_1" -> exchange.reply(200, kycJson)
                    else -> exchange.reply(NOT_FOUND, """{"detail":"not found"}""")
                }
            }
            server.start()
        }

        afterSpec { server.stop(0) }

        fun baseUrl() = "http://127.0.0.1:${server.address.port}"

        "listPayments reads a page of payments" {
            val page = PaymentsClient(baseUrl()).listPayments()
            page.totalElements shouldBe 1
            page.items.single().id shouldBe "pay_1"
        }

        "getPayment keeps the amount precise as a number" {
            val payment = PaymentsClient(baseUrl()).getPayment("pay_1")
            payment.amount shouldBe BigDecimal("125.50")
            payment.currency shouldBe "USD"
            payment.status shouldBe "SETTLED"
        }

        "getPayment sends the bearer token when present" {
            PaymentsClient(baseUrl(), token = "t-123").getPayment("pay_1")
            seenAuthorization shouldBe "Bearer t-123"
        }

        "getPayment throws FincoreException on 404" {
            shouldThrow<FincoreException> { PaymentsClient(baseUrl()).getPayment("missing") }.status shouldBe NOT_FOUND
        }

        "listCases filters by status and reads a bare list" {
            val cases = ComplianceClient(baseUrl()).listCases("OPEN")
            seenCaseQuery shouldBe "status=OPEN"
            cases.single().status shouldBe "OPEN"
        }

        "getCase reads a single case" {
            ComplianceClient(baseUrl()).getCase("case_1").reference shouldBe "c-1"
        }

        "getKycSession reads a single session" {
            val session = ComplianceClient(baseUrl()).getKycSession("kyc_1")
            session.subjectReference shouldBe "subj-1"
            session.status shouldBe "APPROVED"
        }
    })
