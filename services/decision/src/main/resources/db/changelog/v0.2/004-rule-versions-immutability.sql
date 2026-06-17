--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:004-rule-versions-immutability-fn dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION decision.reject_rule_version_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'decision.rule_versions is append-only: % is not permitted', TG_OP
        USING ERRCODE = '0A000';
END;
$$ LANGUAGE plpgsql;

--changeset fincore:004-rule-versions-immutability-trigger dbms:postgresql
DROP TRIGGER IF EXISTS trg_rule_versions_immutable ON decision.rule_versions;
CREATE TRIGGER trg_rule_versions_immutable
    BEFORE UPDATE OR DELETE ON decision.rule_versions
    FOR EACH ROW
    EXECUTE FUNCTION decision.reject_rule_version_mutation();
