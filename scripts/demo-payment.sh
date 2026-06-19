#!/usr/bin/env bash
# SPDX-License-Identifier: BUSL-1.1
# SPDX-FileCopyrightText: 2026 FinCore Engine Authors
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8085}"
PAYMENTS_URL="${PAYMENTS_URL:-http://localhost:8081}"
CLIENT_ID="${FINCORE_DEMO_CLIENT_ID:-fincore-demo}"
CLIENT_SECRET="${FINCORE_DEMO_CLIENT_SECRET:-fincore-demo-secret}"
WEBHOOK_SECRET="${FINCORE_PAYMENTS_WEBHOOK_HMACSECRET:-sandbox-webhook-secret}"
TOKEN_ENDPOINT="$KEYCLOAK_URL/realms/fincore/protocol/openid-connect/token"

uuid() { uuidgen | tr '[:upper:]' '[:lower:]'; }
extract() { sed -n "s/.*\"$1\":\"\\([^\"]*\\)\".*/\\1/p"; }

status_of() {
    curl -fsS -H "Authorization: Bearer $token" "$PAYMENTS_URL/v1/payments/$1" | extract status
}

token=$(
    curl -fsS -X POST "$TOKEN_ENDPOINT" \
        -d grant_type=client_credentials -d "client_id=$CLIENT_ID" -d "client_secret=$CLIENT_SECRET" |
        extract access_token
)
[ -n "$token" ] || {
    echo "FAIL: could not obtain a token"
    exit 1
}

created=$(
    curl -fsS -X POST "$PAYMENTS_URL/v1/payments" \
        -H "Authorization: Bearer $token" \
        -H "Idempotency-Key: $(uuid)" \
        -H "Content-Type: application/json" \
        --data-binary '{"amount":"100.00","currency":"USD","reference":"demo-e2e"}'
)
payment_id=$(printf '%s' "$created" | extract id)
[ -n "$payment_id" ] || {
    echo "FAIL: no payment id in response: $created"
    exit 1
}
echo "initiated $payment_id"

submitted=false
for _ in $(seq 1 30); do
    status=$(status_of "$payment_id")
    [ "$status" = "SUBMITTED" ] && {
        submitted=true
        break
    }
    [ "$status" = "FAILED" ] && {
        echo "FAIL: payment FAILED during orchestration"
        exit 1
    }
    sleep 1
done
[ "$submitted" = true ] || {
    echo "FAIL: payment did not reach SUBMITTED"
    exit 1
}
echo "submitted $payment_id"

body="{\"deliveryId\":\"$(uuid)\",\"providerReference\":\"sbx-$payment_id\",\"outcome\":\"SETTLED\"}"
signature=$(printf '%s' "$body" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" | sed 's/^.*= //')
[ -n "$signature" ] || {
    echo "FAIL: could not compute the webhook signature"
    exit 1
}
curl -fsS -X POST "$PAYMENTS_URL/v1/payments/webhooks" \
    -H "X-Webhook-Signature: $signature" -H "Content-Type: application/json" --data-binary "$body" >/dev/null

settled=false
for _ in $(seq 1 15); do
    [ "$(status_of "$payment_id")" = "SETTLED" ] && {
        settled=true
        break
    }
    sleep 1
done
[ "$settled" = true ] || {
    echo "FAIL: payment did not reach SETTLED after the webhook"
    exit 1
}
echo "payment lifecycle OK: $payment_id INITIATED -> SUBMITTED -> SETTLED"
