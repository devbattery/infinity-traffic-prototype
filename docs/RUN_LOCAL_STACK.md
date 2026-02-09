# 로컬 실행 가이드 (실행/중지/검증)

이 문서는 **Infinity Traffic 전체 스택을 로컬에서 직접 실행**하는 절차를 상세히 설명합니다.

## 0. 전제 조건

- OS: macOS (Apple Silicon 포함)
- JDK: 17 이상
- Docker Desktop 실행 중
- 포트 사용 가능:
  - 앱: `8080`, `8081`, `8082`, `8083`, `8084`
  - 인프라: `2181`, `3000`, `5433`, `5434`, `6379`, `9090`, `9091`, `9092`, `9094`

## 1. 가장 빠른 방법 (권장)

프로젝트 루트(`/Users/ibm/IdeaProjects/infinity-traffic`)에서 아래 1개 명령으로 전체 기동할 수 있습니다.

```bash
./scripts/local/start-all.sh
```

이 스크립트가 수행하는 일:

1. Docker 인프라(`infra/docker-compose.yml`)를 기동
2. 각 서비스 JAR 재빌드
3. 기존 포트 점유 프로세스 정리
4. 서비스 5개를 실행
5. `/actuator/health`로 준비 상태 확인

실행 중 로그:

- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/auth-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-command-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-query-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/api-gateway.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-frontend.log`

## 2. 상태 확인

```bash
./scripts/local/status-all.sh
```

정상 예시:

- `api-gateway:8080 UP`
- `auth-service:8081 UP`
- `traffic-command-service:8082 UP`
- `traffic-query-service:8083 UP`
- `traffic-frontend:8084 UP`

추가 수동 확인:

```bash
curl http://localhost:8084/actuator/health
curl http://localhost:8084/dashboard
curl 'http://localhost:8084/ui/api/dashboard?limit=10'
```

## 3. 접속 주소

- 운영 대시보드(Thymeleaf): [http://localhost:8084/dashboard](http://localhost:8084/dashboard)
- API Gateway: [http://localhost:8080](http://localhost:8080)
- Kafka UI: [http://localhost:9090](http://localhost:9090)
- Prometheus: [http://localhost:9091](http://localhost:9091)
- Grafana: [http://localhost:3000](http://localhost:3000)

## 4. 중지 방법

애플리케이션(5개 서비스)만 중지:

```bash
./scripts/local/stop-all.sh
```

인프라까지 모두 중지:

```bash
docker compose -f /Users/ibm/IdeaProjects/infinity-traffic/infra/docker-compose.yml down
```

## 5. 수동 실행 방법 (스크립트 없이)

### 5-1. 인프라 실행

```bash
docker compose -f /Users/ibm/IdeaProjects/infinity-traffic/infra/docker-compose.yml up -d
```

### 5-2. 빌드

```bash
./gradlew :auth-service:bootJar :traffic-command-service:bootJar :traffic-query-service:bootJar :api-gateway:bootJar :traffic-frontend:bootJar
```

### 5-3. 서비스 실행 (터미널 5개)

```bash
java -jar /Users/ibm/IdeaProjects/infinity-traffic/services/auth-service/build/libs/auth-service-0.1.0-SNAPSHOT.jar
```

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9094 java -jar /Users/ibm/IdeaProjects/infinity-traffic/services/traffic-command-service/build/libs/traffic-command-service-0.1.0-SNAPSHOT.jar
```

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9094 java -jar /Users/ibm/IdeaProjects/infinity-traffic/services/traffic-query-service/build/libs/traffic-query-service-0.1.0-SNAPSHOT.jar
```

```bash
java -jar /Users/ibm/IdeaProjects/infinity-traffic/services/api-gateway/build/libs/api-gateway-0.1.0-SNAPSHOT.jar
```

```bash
java -jar /Users/ibm/IdeaProjects/infinity-traffic/services/traffic-frontend/build/libs/traffic-frontend-0.1.0-SNAPSHOT.jar
```

## 6. 부하 테스트 실행

서비스가 정상 기동된 상태에서:

```bash
BASE_URL=http://localhost:8080 ./performance/k6/run-all.sh
```

결과 파일:

- JSON: `/Users/ibm/IdeaProjects/infinity-traffic/performance/results/*.json`
- 로그: `/Users/ibm/IdeaProjects/infinity-traffic/performance/results/*.log`
- 리포트: `/Users/ibm/IdeaProjects/infinity-traffic/docs/performance/K6_REPORT.md`

## 7. 자주 발생하는 문제

### 7-1. 포트 충돌

증상: `Address already in use`

대응:

```bash
./scripts/local/stop-all.sh
./scripts/local/start-all.sh
```

### 7-2. Kafka 연결 실패

증상: `LEADER_NOT_AVAILABLE` 또는 consumer/producer timeout

대응:

1. `docker compose -f infra/docker-compose.yml ps`로 `kafka` 상태 확인
2. 커맨드/쿼리 서비스 실행 시 `KAFKA_BOOTSTRAP_SERVERS=localhost:9094` 사용 확인

### 7-3. 프론트 화면은 열리는데 데이터가 비어 있음

증상: `/dashboard` 진입 가능하지만 카드/테이블 비어 있음

대응:

1. 게이트웨이/쿼리 서비스 health 확인
2. 이벤트 수집 폼으로 1~2건 입력 후 다시 확인
3. `traffic-query-service` 로그에서 Kafka consume 여부 확인

## 8. 참고 스크립트

- 시작: `/Users/ibm/IdeaProjects/infinity-traffic/scripts/local/start-all.sh`
- 상태: `/Users/ibm/IdeaProjects/infinity-traffic/scripts/local/status-all.sh`
- 중지: `/Users/ibm/IdeaProjects/infinity-traffic/scripts/local/stop-all.sh`
