#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PID_DIR="$ROOT_DIR/.runtime/pids"

# PID 파일을 기준으로 애플리케이션 프로세스를 종료한다.
stop_by_pid_files() {
  local services=(auth-service traffic-command-service traffic-query-service api-gateway traffic-frontend)
  for service in "${services[@]}"; do
    local pid_file="$PID_DIR/$service.pid"
    if [[ -f "$pid_file" ]]; then
      local pid
      pid="$(cat "$pid_file")"
      if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        echo "[stop-all] stopping $service (pid=$pid)"
        kill "$pid" 2>/dev/null || true
        sleep 1
        kill -9 "$pid" 2>/dev/null || true
      fi
      rm -f "$pid_file"
    fi
  done
}

# 지정 포트의 점유 프로세스를 정리한다.
stop_ports() {
  local ports=(8080 8081 8082 8083 8084)
  for port in "${ports[@]}"; do
    local pids
    pids="$(lsof -ti tcp:"$port" || true)"
    if [[ -n "$pids" ]]; then
      echo "[stop-all] kill port $port pids: $pids"
      for pid in $pids; do
        kill "$pid" 2>/dev/null || true
      done
      sleep 1
      for pid in $pids; do
        kill -9 "$pid" 2>/dev/null || true
      done
    fi
  done
}

cd "$ROOT_DIR"

stop_by_pid_files
stop_ports

echo "[stop-all] application services stopped"

echo "[stop-all] if you also want infra down, run:"
echo "  docker compose -f $ROOT_DIR/infra/docker-compose.yml down"
