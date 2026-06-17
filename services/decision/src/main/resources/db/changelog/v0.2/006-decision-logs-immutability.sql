--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:006-decision-logs-immutability-fn dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION decision.reject_decision_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'decision.decision_logs is append-only: % is not permitted', TG_OP
        USING ERRCODE = '0A000';
END;
$$ LANGUAGE plpgsql;

--changeset fincore:006-decision-logs-immutability-trigger dbms:postgresql
DROP TRIGGER IF EXISTS trg_decision_logs_immutable ON decision.decision_logs;
CREATE TRIGGER trg_decision_logs_immutable
    BEFORE UPDATE OR DELETE ON decision.decision_logs
    FOR EACH ROW
    EXECUTE FUNCTION decision.reject_decision_log_mutation();
