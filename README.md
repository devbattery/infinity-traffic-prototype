# Infinity Traffic

실무형 Kotlin + Spring Boot MSA 기반의 **실시간 교통 이벤트 수집/집계 플랫폼**입니다.

- 진입점: `api-gateway`
- 인증: `auth-service`
- 쓰기(CQRS Command): `traffic-command-service`
- 읽기(CQRS Query): `traffic-query-service`
- 운영 대시보드(React SPA): `traffic-frontend`
- 공통 계약/추적: `shared-kernel`

상세 구현 순서(A→Z)와 동작 원리는 아래 문서를 확인하세요.

- [A부터 Z까지 구현/작동 방식](docs/IMPLEMENTATION_A_TO_Z.md)
- [A부터 Z까지 앱 사용/검증 설명서](docs/APP_MANUAL_A_TO_Z.md)
- [기술 스택 기초→실무 응용 완전 학습서](docs/TECH_STACK_ZERO_TO_PRACTICAL_GUIDE_KR.md)
- [인수 테스트 시나리오](docs/ACCEPTANCE_TESTS.md)
- [k6 부하 테스트 리포트](docs/performance/K6_REPORT.md)
- [로컬 실행 상세 가이드](docs/RUN_LOCAL_STACK.md)

## 빠른 시작

### 사전 요구사항

- `JDK 17` 설치 및 `JAVA_HOME` 설정
- Gradle은 반드시 저장소의 Wrapper(`./gradlew`) 사용

### 1) 빌드/테스트

```bash
./gradlew test
```

### 1-1) 프론트엔드(UI) 번들 갱신

```bash
cd services/traffic-frontend/ui
npm install
npm run build
```

### 2) 인프라 실행(Kafka/PostgreSQL/Redis/Prometheus/Grafana)

```bash
cd infra
docker compose up -d
```

### 3) 서비스 실행 예시

```bash
./gradlew :api-gateway:bootRun
./gradlew :auth-service:bootRun
./gradlew :traffic-command-service:bootRun
./gradlew :traffic-query-service:bootRun
./gradlew :traffic-frontend:bootRun
```

## 포트

- `api-gateway`: `8080`
- `auth-service`: `8081`
- `traffic-command-service`: `8082`
- `traffic-query-service`: `8083`
- `traffic-frontend`: `8084`
- `kafka-ui`: `9090`
- `prometheus`: `9091`
- `grafana`: `3000`

## k6 부하 테스트

```bash
BASE_URL=http://localhost:8080 ./performance/k6/run-all.sh
```
