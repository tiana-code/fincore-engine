--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:013-invariant-fn dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION ledger.verify_double_entry_invariant()
RETURNS TRIGGER AS $$
DECLARE
    v_sum NUMERIC(38, 18);
BEGIN
    SELECT COALESCE(SUM(amount), 0)
    INTO   v_sum
    FROM   ledger.entries
    WHERE  transaction_id = NEW.transaction_id
      AND  currency       = NEW.currency;

    IF v_sum <> 0 THEN
        RAISE EXCEPTION
            'double-entry invariant violated: transaction=%, currency=%, sum=%',
            NEW.transaction_id, NEW.currency, v_sum
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

--changeset fincore:013-invariant-trigger dbms:postgresql
DROP TRIGGER IF EXISTS trg_entries_invariant ON ledger.entries;
CREATE CONSTRAINT TRIGGER trg_entries_invariant
    AFTER INSERT ON ledger.entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION ledger.verify_double_entry_invariant();
