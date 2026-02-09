#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RESULT_DIR="$ROOT_DIR/performance/results"
mkdir -p "$RESULT_DIR"

BASE_URL="${BASE_URL:-http://localhost:8080}"
TS="$(date +%Y%m%d_%H%M%S)"

echo "[k6] BASE_URL=$BASE_URL"

echo "[k6] Running auth-flow.js"
k6 run \
  --summary-export "$RESULT_DIR/auth_flow_${TS}.json" \
  -e BASE_URL="$BASE_URL" \
  "$ROOT_DIR/performance/k6/auth-flow.js" | tee "$RESULT_DIR/auth_flow_${TS}.log"

echo "[k6] Running traffic-flow.js"
k6 run \
  --summary-export "$RESULT_DIR/traffic_flow_${TS}.json" \
  -e BASE_URL="$BASE_URL" \
  "$ROOT_DIR/performance/k6/traffic-flow.js" | tee "$RESULT_DIR/traffic_flow_${TS}.log"

echo "[k6] Completed. Results are in $RESULT_DIR"
