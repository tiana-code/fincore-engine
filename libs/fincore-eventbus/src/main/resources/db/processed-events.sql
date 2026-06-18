-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors
--
-- Reference DDL for the consumer dedup table used by JdbcProcessedEventStore. This is NOT a
-- drop-in migration: a consuming service copies this statement into its own Liquibase formatted-SQL
-- changeset (per CLAUDE.md 8.5.1) in its own schema. Idempotent and re-runnable.

CREATE TABLE IF NOT EXISTS processed_events (
    envelope_id    UUID         NOT NULL,
    consumer_group VARCHAR(128) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_processed_events PRIMARY KEY (envelope_id, consumer_group)
);
