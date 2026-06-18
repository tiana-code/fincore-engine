// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.time.Instant

class EventEnvelopeTest {
    private data class SamplePayload(
        val ref: String,
        val amount: Int,
    )

    private val mapper: ObjectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private fun sampleEnvelope(): EventEnvelope<SamplePayload> =
        EventEnvelope.of(
            source = "ledger",
            type = LedgerEvents.TransactionPosted,
            data = SamplePayload(ref = "ref-1", amount = 100),
            subject = "tx_01",
            correlationId = "corr-1",
        )

    @Test
    fun `should round-trip an envelope without losing any attribute when serialized then deserialized`() {
        val original = sampleEnvelope()

        val json = mapper.writeValueAsString(original)
        val restored = mapper.readValue<EventEnvelope<SamplePayload>>(json)

        restored shouldBe original
    }

    @Test
    fun `should serialize required CloudEvents attribute names with specversion 1_0 when serialized`() {
        val tree = mapper.readTree(mapper.writeValueAsString(sampleEnvelope()))

        tree.has("id") shouldBe true
        tree.has("source") shouldBe true
        tree.has("specversion") shouldBe true
        tree.has("type") shouldBe true
        tree.has("time") shouldBe true
        tree.has("datacontenttype") shouldBe true
        tree.has("data") shouldBe true
        tree.get("specversion").asText() shouldBe "1.0"
    }

    @Test
    fun `should default datacontenttype to application json when not supplied`() {
        sampleEnvelope().datacontenttype shouldBe "application/json"
    }

    @Test
    fun `should build a self-versioning type from the event type when constructed`() {
        sampleEnvelope().type shouldBe "com.fincore.ledger.transaction.posted.v1"
    }

    @Test
    fun `should serialize tracing ids under lowercase names and round-trip them when present`() {
        val envelope =
            EventEnvelope.of(
                source = "ledger",
                type = LedgerEvents.TransactionPosted,
                data = SamplePayload(ref = "ref-1", amount = 1),
                correlationId = "corr-9",
                causationId = "cause-9",
            )

        val tree = mapper.readTree(mapper.writeValueAsString(envelope))
        tree.get("correlationid").asText() shouldBe "corr-9"
        tree.get("causationid").asText() shouldBe "cause-9"

        val restored = mapper.readValue<EventEnvelope<SamplePayload>>(mapper.writeValueAsString(envelope))
        restored.correlationid shouldBe "corr-9"
        restored.causationid shouldBe "cause-9"
    }

    @Test
    fun `should round-trip time as an RFC 3339 string not an epoch array when serialized`() {
        val fixed = Instant.parse("2026-06-17T10:15:30Z")
        val envelope =
            EventEnvelope(
                id = java.util.UUID.randomUUID(),
                source = "ledger",
                specversion = EventEnvelope.SPEC_VERSION,
                type = LedgerEvents.TransactionPosted.fullType,
                time = fixed,
                subject = null,
                datacontenttype = EventEnvelope.DEFAULT_CONTENT_TYPE,
                correlationid = null,
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )

        val json = mapper.writeValueAsString(envelope)
        json shouldContain "\"2026-06-17T10:15:30Z\""

        mapper.readValue<EventEnvelope<SamplePayload>>(json).time shouldBe fixed
    }

    @Test
    fun `should serialize an envelope to a stable canonical json form when attributes are fixed`() {
        val envelope =
            EventEnvelope(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                source = "ledger",
                specversion = EventEnvelope.SPEC_VERSION,
                type = LedgerEvents.TransactionPosted.fullType,
                time = Instant.parse("2026-06-17T10:15:30Z"),
                subject = "tx_01",
                datacontenttype = EventEnvelope.DEFAULT_CONTENT_TYPE,
                correlationid = "corr-1",
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )

        mapper.writeValueAsString(envelope) shouldBe
            "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"source\":\"ledger\"," +
            "\"specversion\":\"1.0\",\"type\":\"com.fincore.ledger.transaction.posted.v1\"," +
            "\"time\":\"2026-06-17T10:15:30Z\",\"subject\":\"tx_01\"," +
            "\"datacontenttype\":\"application/json\",\"correlationid\":\"corr-1\"," +
            "\"data\":{\"ref\":\"r\",\"amount\":1}}"
    }

    @Test
    fun `should reject a blank source when constructed`() {
        shouldThrow<IllegalArgumentException> {
            EventEnvelope(
                id = java.util.UUID.randomUUID(),
                source = " ",
                specversion = EventEnvelope.SPEC_VERSION,
                type = LedgerEvents.TransactionPosted.fullType,
                time = Instant.now(),
                subject = null,
                datacontenttype = EventEnvelope.DEFAULT_CONTENT_TYPE,
                correlationid = null,
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )
        }
    }

    @Test
    fun `should reject a blank type when constructed`() {
        shouldThrow<IllegalArgumentException> {
            EventEnvelope(
                id = java.util.UUID.randomUUID(),
                source = "ledger",
                specversion = EventEnvelope.SPEC_VERSION,
                type = " ",
                time = Instant.now(),
                subject = null,
                datacontenttype = EventEnvelope.DEFAULT_CONTENT_TYPE,
                correlationid = null,
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )
        }
    }

    @Test
    fun `should reject a blank specversion when constructed`() {
        shouldThrow<IllegalArgumentException> {
            EventEnvelope(
                id = java.util.UUID.randomUUID(),
                source = "ledger",
                specversion = " ",
                type = LedgerEvents.TransactionPosted.fullType,
                time = Instant.now(),
                subject = null,
                datacontenttype = EventEnvelope.DEFAULT_CONTENT_TYPE,
                correlationid = null,
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )
        }
    }

    @Test
    fun `should reject a blank datacontenttype when constructed`() {
        shouldThrow<IllegalArgumentException> {
            EventEnvelope(
                id = java.util.UUID.randomUUID(),
                source = "ledger",
                specversion = EventEnvelope.SPEC_VERSION,
                type = LedgerEvents.TransactionPosted.fullType,
                time = Instant.now(),
                subject = null,
                datacontenttype = " ",
                correlationid = null,
                causationid = null,
                data = SamplePayload(ref = "r", amount = 1),
            )
        }
    }
}
