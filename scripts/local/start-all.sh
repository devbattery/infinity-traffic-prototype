#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime"
LOG_DIR="$RUNTIME_DIR/logs"
PID_DIR="$RUNTIME_DIR/pids"
FRONTEND_UI_DIR="$ROOT_DIR/services/traffic-frontend/ui"

mkdir -p "$LOG_DIR" "$PID_DIR"

# PID 파일로 등록된 이전 프로세스가 있으면 먼저 정리한다.
stop_registered_processes() {
  for service in auth-service traffic-command-service traffic-query-service api-gateway traffic-frontend; do
    local pid_file="$PID_DIR/$service.pid"
    if [[ -f "$pid_file" ]]; then
      local pid
      pid="$(cat "$pid_file")"
      if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
        sleep 1
        kill -9 "$pid" 2>/dev/null || true
      fi
      rm -f "$pid_file"
    fi
  done
}

# 포트 점유 프로세스를 찾아 종료한다.
stop_ports() {
  local ports=(8080 8081 8082 8083 8084)
  for port in "${ports[@]}"; do
    local pids
    pids="$(lsof -ti tcp:"$port" || true)"
    if [[ -n "$pids" ]]; then
      echo "[start-all] kill port $port pids: $pids"
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

# 지정 포트의 헬스 체크가 통과할 때까지 기다린다.
wait_health() {
  local service="$1"
  local port="$2"
  local path="${3:-/actuator/health}"

  echo "[start-all] waiting health: $service ($port$path)"
  for _ in {1..90}; do
    local code
    code="$(curl -s -o /tmp/"$service"_health.json -w '%{http_code}' "http://localhost:$port$path" || true)"
    if [[ "$code" == "200" ]]; then
      echo "[start-all] healthy: $service"
      return 0
    fi
    sleep 1
  done

  echo "[start-all] health check failed: $service"
  return 1
}

# React SPA 정적 번들을 항상 최신 상태로 맞춘다.
# - package-lock 변경 시 의존성을 재설치하고
# - 매 실행마다 npm build를 수행해 Spring 정적 리소스를 갱신한다.
build_frontend_bundle() {
  if [[ ! -f "$FRONTEND_UI_DIR/package.json" ]]; then
    echo "[start-all] frontend package.json not found: $FRONTEND_UI_DIR"
    exit 1
  fi

  if ! command -v npm >/dev/null 2>&1; then
    echo "[start-all] npm is required but not found in PATH"
    exit 1
  fi

  if [[ ! -d "$FRONTEND_UI_DIR/node_modules" || "$FRONTEND_UI_DIR/package-lock.json" -nt "$FRONTEND_UI_DIR/node_modules" ]]; then
    echo "[start-all] install frontend dependencies"
    (cd "$FRONTEND_UI_DIR" && npm install --no-audit --no-fund)
  else
    echo "[start-all] frontend dependencies are up to date"
  fi

  echo "[start-all] build frontend static bundle"
  (cd "$FRONTEND_UI_DIR" && npm run build)
}

start_service() {
  local service="$1"
  local cmd="$2"
  local log_file="$LOG_DIR/$service.log"
  local pid_file="$PID_DIR/$service.pid"

  echo "[start-all] start $service"
  nohup bash -lc "$cmd" > "$log_file" 2>&1 &
  echo "$!" > "$pid_file"
}

cd "$ROOT_DIR"

echo "[start-all] ensure infra is running"
docker compose -f infra/docker-compose.yml up -d > /tmp/infinity_infra_up.log 2>&1 || {
  cat /tmp/infinity_infra_up.log
  exit 1
}

build_frontend_bundle

echo "[start-all] build application jars"
./gradlew :auth-service:bootJar :traffic-command-service:bootJar :traffic-query-service:bootJar :api-gateway:bootJar :traffic-frontend:bootJar

stop_registered_processes
stop_ports

start_service "auth-service" "java -jar $ROOT_DIR/services/auth-service/build/libs/auth-service-0.1.0-SNAPSHOT.jar"
start_service "traffic-command-service" "KAFKA_BOOTSTRAP_SERVERS=localhost:9094 java -jar $ROOT_DIR/services/traffic-command-service/build/libs/traffic-command-service-0.1.0-SNAPSHOT.jar"
start_service "traffic-query-service" "KAFKA_BOOTSTRAP_SERVERS=localhost:9094 java -jar $ROOT_DIR/services/traffic-query-service/build/libs/traffic-query-service-0.1.0-SNAPSHOT.jar"
start_service "api-gateway" "java -jar $ROOT_DIR/services/api-gateway/build/libs/api-gateway-0.1.0-SNAPSHOT.jar"
start_service "traffic-frontend" "java -jar $ROOT_DIR/services/traffic-frontend/build/libs/traffic-frontend-0.1.0-SNAPSHOT.jar"

wait_health "auth-service" 8081
wait_health "traffic-command-service" 8082
wait_health "traffic-query-service" 8083
wait_health "api-gateway" 8080
wait_health "traffic-frontend" 8084

echo "[start-all] all services are up"
echo "[start-all] frontend: http://localhost:8084/dashboard"
echo "[start-all] gateway:  http://localhost:8080/api"
