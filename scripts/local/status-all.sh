#!/usr/bin/env bash
set -euo pipefail

# 주요 서비스 헬스 상태와 포트 리스닝 상태를 출력한다.
check_health() {
  local name="$1"
  local port="$2"
  local code
  code="$(curl -s -o /tmp/${name}_health.json -w '%{http_code}' "http://localhost:${port}/actuator/health" || true)"
  if [[ "$code" == "200" ]]; then
    echo "[status] $name:$port UP"
  else
    echo "[status] $name:$port DOWN(code=$code)"
  fi
}

check_health "api-gateway" 8080
check_health "auth-service" 8081
check_health "traffic-command-service" 8082
check_health "traffic-query-service" 8083
check_health "traffic-frontend" 8084

echo "[status] listening ports"
for p in 8080 8081 8082 8083 8084; do
  if lsof -nP -iTCP:$p -sTCP:LISTEN >/dev/null 2>&1; then
    echo "  - $p LISTEN"
  else
    echo "  - $p CLOSED"
  fi
done
