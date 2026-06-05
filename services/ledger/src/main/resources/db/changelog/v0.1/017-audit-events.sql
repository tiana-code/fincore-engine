--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:017-audit-events dbms:postgresql
CREATE TABLE IF NOT EXISTS platform.audit_events (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    actor_id       VARCHAR(64)  NOT NULL,
    correlation_id VARCHAR(64)  NOT NULL,
    action         VARCHAR(64)  NOT NULL,
    resource_type  VARCHAR(32)  NOT NULL,
    resource_id    VARCHAR(64)  NOT NULL,
    result         VARCHAR(16)  NOT NULL,
    request_hash   VARCHAR(64),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_audit_events PRIMARY KEY (id),
    CONSTRAINT ck_audit_events_result
        CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource
    ON platform.audit_events(resource_type, resource_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor
    ON platform.audit_events(actor_id);
