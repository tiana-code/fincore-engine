#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
READINESS="$BASE_URL/actuator/health/readiness"
LIVENESS="$BASE_URL/actuator/health/liveness"
API_DOCS="$BASE_URL/v3/api-docs"

attempts=15
interval=2

ready=false
for i in $(seq 1 "$attempts"); do
  if curl -fsS -o /dev/null "$READINESS"; then
    ready=true
    break
  fi
  [ "$i" -lt "$attempts" ] && sleep "$interval"
done

if [ "$ready" != true ]; then
  echo "FAIL: ledger readiness not UP within $((attempts * interval))s at $READINESS"
  exit 1
fi

if ! curl -fsS -o /dev/null "$LIVENESS"; then
  echo "FAIL: ledger liveness not UP at $LIVENESS"
  exit 1
fi

code=$(curl -sS -o /dev/null -w '%{http_code}' "$API_DOCS")
if [ "$code" != "200" ]; then
  echo "FAIL: api-docs returned $code at $API_DOCS"
  exit 1
fi

echo "ledger smoke OK: readiness UP, liveness UP, api-docs 200"
