--liquibase formatted sql
-- SPDX-License-Identifier: BUSL-1.1
-- SPDX-FileCopyrightText: 2026 FinCore Engine Authors

--changeset fincore:001-schemas dbms:postgresql
CREATE SCHEMA IF NOT EXISTS ledger;
CREATE SCHEMA IF NOT EXISTS platform;

--changeset fincore:001-set-updated-at-fn dbms:postgresql runOnChange:true splitStatements:false
CREATE OR REPLACE FUNCTION platform.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
