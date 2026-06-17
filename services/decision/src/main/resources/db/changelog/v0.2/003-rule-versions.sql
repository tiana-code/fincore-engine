--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:003-rule-versions dbms:postgresql
CREATE TABLE IF NOT EXISTS decision.rule_versions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    rule_id     UUID        NOT NULL,
    version_no  INTEGER     NOT NULL,
    dsl         JSONB       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_rule_versions PRIMARY KEY (id),
    CONSTRAINT fk_rule_versions_rule
        FOREIGN KEY (rule_id) REFERENCES decision.decision_rules(id),
    CONSTRAINT uq_rule_versions_rule_version UNIQUE (rule_id, version_no)
);
CREATE INDEX IF NOT EXISTS idx_rule_versions_rule
    ON decision.rule_versions(rule_id);

--changeset fincore:003-decision-rules-active-version-fk dbms:postgresql runOnChange:true splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_decision_rules_active_version'
          AND conrelid = 'decision.decision_rules'::regclass
    ) THEN
        ALTER TABLE decision.decision_rules
            ADD CONSTRAINT fk_decision_rules_active_version
            FOREIGN KEY (active_version_id) REFERENCES decision.rule_versions(id);
    END IF;
END $$;
