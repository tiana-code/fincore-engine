#!/usr/bin/env bash
# SPDX-License-Identifier: BUSL-1.1
# SPDX-FileCopyrightText: 2026 FinCore Engine Authors
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8085}"
LEDGER_URL="${LEDGER_URL:-http://localhost:8080}"
PAYMENTS_URL="${PAYMENTS_URL:-http://localhost:8081}"
CLIENT_ID="${FINCORE_DEMO_CLIENT_ID:-fincore-demo}"
CLIENT_SECRET="${FINCORE_DEMO_CLIENT_SECRET:-fincore-demo-secret}"
TOKEN_ENDPOINT="$KEYCLOAK_URL/realms/fincore/protocol/openid-connect/token"

fetch_token() {
    curl -fsS -X POST "$TOKEN_ENDPOINT" \
        -d grant_type=client_credentials \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET" |
        sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
}

token=""
for attempt in $(seq 1 5); do
    token=$(fetch_token || true)
    [ -n "$token" ] && break
    echo "token fetch attempt $attempt failed; retrying in 2s"
    sleep 2
done

if [ -z "$token" ]; then
    echo "FAIL: could not obtain a client_credentials token from $TOKEN_ENDPOINT"
    exit 1
fi

ledger_code=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$LEDGER_URL/v1/accounts")
if [ "$ledger_code" != "200" ]; then
    echo "FAIL: GET /v1/accounts with a bearer returned $ledger_code (expected 200)"
    exit 1
fi

payments_code=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" "$PAYMENTS_URL/v1/payments")
if [ "$payments_code" != "200" ]; then
    echo "FAIL: GET /v1/payments with a bearer returned $payments_code (expected 200)"
    exit 1
fi

echo "auth chain OK: client_credentials token accepted by ledger and payments"
