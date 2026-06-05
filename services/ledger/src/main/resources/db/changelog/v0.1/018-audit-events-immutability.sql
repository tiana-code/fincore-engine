--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:018-audit-immutability-fn dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION platform.reject_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'platform.audit_events is append-only: % is not permitted', TG_OP
        USING ERRCODE = '0A000';
END;
$$ LANGUAGE plpgsql;

--changeset fincore:018-audit-immutability-trigger dbms:postgresql
DROP TRIGGER IF EXISTS trg_audit_events_immutable ON platform.audit_events;
CREATE TRIGGER trg_audit_events_immutable
    BEFORE UPDATE OR DELETE ON platform.audit_events
    FOR EACH ROW
    EXECUTE FUNCTION platform.reject_audit_mutation();
