#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-default}"
SUBMISSION_ID="${2:-sub-persist-smoke-$(date +%s)}"
PARTNER_ID="${3:-partner-smoke}"
PORT="${4:-8088}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1"
    exit 1
  fi
}

require_cmd kubectl
require_cmd curl

echo "Starting port-forward to admin-bff on localhost:${PORT}"
kubectl -n "$NAMESPACE" port-forward svc/admin-bff "${PORT}:80" >/tmp/admin-bff-portforward.log 2>&1 &
PF_PID=$!
cleanup() {
  if kill -0 "$PF_PID" >/dev/null 2>&1; then
    kill "$PF_PID" >/dev/null 2>&1 || true
    wait "$PF_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

sleep 2

echo "POST /admin/trigger for ${SUBMISSION_ID}"
POST_RESPONSE=$(curl -sS -i -X POST "http://localhost:${PORT}/admin/trigger" \
  -H 'Content-Type: application/json' \
  -d "{\"submissionId\":\"${SUBMISSION_ID}\",\"partnerId\":\"${PARTNER_ID}\"}")
echo "$POST_RESPONSE"

echo "GET /admin/jobs before postgres restart"
BEFORE_RESPONSE=$(curl -sS -i "http://localhost:${PORT}/admin/jobs")
echo "$BEFORE_RESPONSE"

if ! echo "$BEFORE_RESPONSE" | grep -q "$SUBMISSION_ID"; then
  echo "Expected submission id '${SUBMISSION_ID}' not found before restart"
  exit 1
fi

PG_POD=$(kubectl -n "$NAMESPACE" get pods -l app=orchestrator-postgres -o jsonpath='{.items[0].metadata.name}')
echo "Restarting postgres pod: ${PG_POD}"
kubectl -n "$NAMESPACE" delete pod "$PG_POD" --wait=true >/dev/null
kubectl -n "$NAMESPACE" wait --for=condition=Ready pod -l app=orchestrator-postgres --timeout=120s >/dev/null

echo "GET /admin/jobs after postgres restart"
AFTER_RESPONSE=$(curl -sS -i "http://localhost:${PORT}/admin/jobs")
echo "$AFTER_RESPONSE"

if ! echo "$AFTER_RESPONSE" | grep -q "$SUBMISSION_ID"; then
  echo "Expected submission id '${SUBMISSION_ID}' not found after restart"
  exit 1
fi

echo "Persistence smoke test passed"
