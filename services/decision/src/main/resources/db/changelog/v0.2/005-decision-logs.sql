--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:005-decision-logs dbms:postgresql
CREATE TABLE IF NOT EXISTS decision.decision_logs (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    evaluated_at    TIMESTAMPTZ NOT NULL,
    rule_version_id UUID        NOT NULL,
    input_hash      VARCHAR(64) NOT NULL,
    matched         BOOLEAN     NOT NULL,
    outcome_label   VARCHAR(128),
    reason_codes    JSONB,
    trace           JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_decision_logs PRIMARY KEY (id, evaluated_at),
    CONSTRAINT fk_decision_logs_rule_version
        FOREIGN KEY (rule_version_id) REFERENCES decision.rule_versions(id) ON DELETE RESTRICT,
    CONSTRAINT ck_decision_logs_input_hash CHECK (input_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_decision_logs_outcome CHECK (
        (matched AND outcome_label IS NOT NULL)
        OR (NOT matched AND outcome_label IS NULL AND reason_codes IS NULL)
    )
) PARTITION BY RANGE (evaluated_at);

CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_06 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_07 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_08 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_09 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_10 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_11 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2026_12 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS decision.decision_logs_2027_01 PARTITION OF decision.decision_logs
    FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');

CREATE TABLE IF NOT EXISTS decision.decision_logs_default PARTITION OF decision.decision_logs DEFAULT;

CREATE INDEX IF NOT EXISTS idx_decision_logs_rule_version
    ON decision.decision_logs(rule_version_id, evaluated_at DESC);
CREATE INDEX IF NOT EXISTS idx_decision_logs_input_hash
    ON decision.decision_logs(input_hash);
