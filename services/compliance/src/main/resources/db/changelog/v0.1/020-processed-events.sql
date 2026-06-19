--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

-- Consumer dedup ledger for JdbcProcessedEventStore. Lives in the default (public) schema, unqualified, because the
-- store's table-name guard forbids a schema-qualified name; create and runtime insert must resolve to the same schema,
-- so do NOT set a connection currentSchema / hibernate default_schema for this service.
--changeset fincore:compliance-020-processed-events dbms:postgresql
CREATE TABLE IF NOT EXISTS processed_events (
    envelope_id    UUID         NOT NULL,
    consumer_group VARCHAR(120) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_processed_events PRIMARY KEY (envelope_id, consumer_group)
);
