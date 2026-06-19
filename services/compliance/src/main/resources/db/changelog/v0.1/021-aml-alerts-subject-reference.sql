--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

-- No column default: aml_alerts is insert-only and every alert is written with its subject reference, so a blank
-- value is never valid (no default to silently poison the column).
--changeset fincore:compliance-021-aml-alerts-subject-reference dbms:postgresql
ALTER TABLE compliance.aml_alerts
    ADD COLUMN IF NOT EXISTS subject_reference VARCHAR(140) NOT NULL;

CREATE INDEX IF NOT EXISTS ix_aml_alerts_subject_reference ON compliance.aml_alerts (subject_reference);
