# Infinity Traffic 앱 사용/검증 설명서 (A to Z)

이 문서는 이 프로젝트를 처음 보는 사람도 **앱을 실행하고, 이벤트를 넣고, 결과를 검증**할 수 있도록 만든 실무형 운영 매뉴얼입니다.

## A. 앱의 정체성

이 앱은 지도 내비게이션 서비스가 아니라, 다음을 검증하기 위한 운영 콘솔형 앱입니다.

- 대규모 트래픽을 고려한 MSA 분리 구조
- 게이트웨이 라우팅
- 인증(JWT)
- 이벤트 기반 비동기 처리(Kafka)
- CQRS(Command/Query) 분리
- 관측성(Actuator/Prometheus/Grafana)

핵심은 "지도 UI"가 아니라 "백엔드 아키텍처와 데이터 흐름"입니다.

## B. 앱에서 자주 쓰는 용어

- `Event`: 도로 관측 데이터 1건
- `Command`: 이벤트를 쓰는 경로(수집)
- `Query`: 이벤트를 읽는 경로(조회)
- `Trace ID`: 요청 추적용 식별자 (`X-Trace-Id`)
- `Region`: 지역 코드(`SEOUL`, `BUSAN`, `INCHEON`, `DAEJEON`, `GWANGJU`)

## C. 시스템 구성

- `traffic-frontend` (`8084`): 운영 화면(Thymeleaf)
- `api-gateway` (`8080`): 외부 진입 API
- `auth-service` (`8081`): 회원가입/로그인/토큰검증
- `traffic-command-service` (`8082`): 이벤트 수집/발행
- `traffic-query-service` (`8083`): 이벤트 소비/조회
- `Kafka` (`9094` host): 이벤트 브로커

요청 흐름:

1. 프론트가 게이트웨이 호출
2. 게이트웨이가 도메인 서비스로 라우팅
3. 커맨드 서비스가 Kafka에 이벤트 발행
4. 쿼리 서비스가 이벤트 소비 후 조회 모델 갱신
5. 프론트가 요약/최근 이벤트 조회

## D. 실행 전에 확인할 것

- Docker Desktop 실행
- JDK 17+ 설치
- 포트 사용 가능: `8080~8084`, `9094`
- 프로젝트 루트: `/Users/ibm/IdeaProjects/infinity-traffic`

## E. 전체 실행 (권장)

```bash
cd /Users/ibm/IdeaProjects/infinity-traffic
./scripts/local/start-all.sh
```

이 스크립트는 인프라 실행 + 서비스 빌드 + 앱 기동 + 헬스체크까지 수행합니다.

## F. 상태 확인

```bash
./scripts/local/status-all.sh
```

정상 기준:

- `api-gateway:8080 UP`
- `auth-service:8081 UP`
- `traffic-command-service:8082 UP`
- `traffic-query-service:8083 UP`
- `traffic-frontend:8084 UP`

## G. 접속 URL

- 앱 메인: [http://localhost:8084/](http://localhost:8084/)
- 대시보드 직접: [http://localhost:8084/dashboard](http://localhost:8084/dashboard)
- 게이트웨이: [http://localhost:8080](http://localhost:8080)
- Kafka UI: [http://localhost:9090](http://localhost:9090)
- Prometheus: [http://localhost:9091](http://localhost:9091)
- Grafana: [http://localhost:3000](http://localhost:3000)

## H. 화면에서 해야 할 기본 사용 순서

1. 대시보드 접속
2. 회원가입
3. 로그인
4. 이벤트 수집 폼에 데이터 입력(한글 도로명 가능)
5. 지역 필터/조회 개수로 결과 확인
6. 상단 KPI/테이블 자동 갱신(5초 주기) 확인

## I. 회원가입 검증

### UI

- `인증 센터 > 회원가입`
- 예시:
  - 아이디: `operator01`
  - 비밀번호: `Password!1234`

### API 직접 검증

```bash
curl -i -X POST 'http://localhost:8080/api/auth/register' \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: manual-auth-register-001' \
  -d '{"username":"operator01","password":"Password!1234"}'
```

기대 결과:

- HTTP `200`
- JSON에 `username`, `createdAt`

## J. 로그인/토큰 검증

### 로그인

```bash
curl -s -X POST 'http://localhost:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: manual-auth-login-001' \
  -d '{"username":"operator01","password":"Password!1234"}'
```

기대 결과:

- `accessToken` 발급
- `tokenType` = `Bearer`

### 토큰 검증

```bash
TOKEN='여기에_로그인_응답_accessToken_값'
curl -i 'http://localhost:8080/api/auth/validate' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Trace-Id: manual-auth-validate-001'
```

기대 결과:

- HTTP `200`
- `valid: true`
- `username` 포함

## K. 이벤트 넣기 (UI)

대시보드 `이벤트 수집` 패널에서 입력합니다.

예시 입력:

- 지역: `서울 (SEOUL)`
- 도로명: `강변북로`
- 평균 속도: `45`
- 혼잡도: `3`

기대 결과:

- 상단 플래시 메시지: `이벤트가 수집되었습니다. eventId=...`
- 수 초 내 `지역별 요약`과 `최근 이벤트` 반영

## L. 이벤트 넣기 (API)

```bash
curl -i -X POST 'http://localhost:8080/api/traffic/events' \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: manual-traffic-ingest-001' \
  -d '{
    "region":"SEOUL",
    "roadName":"강변북로",
    "averageSpeedKph":45,
    "congestionLevel":3,
    "observedAt":"2026-02-09T15:00:00Z"
  }'
```

기대 결과:

- HTTP `202 Accepted`
- JSON: `eventId`, `status`, `observedAt`

## M. 이벤트 반영 확인 (조회 API)

### 요약 조회

```bash
curl -s 'http://localhost:8080/api/traffic/summary?region=SEOUL' \
  -H 'X-Trace-Id: manual-traffic-summary-001'
```

기대 결과:

- `totalEvents` 증가
- `regions[]`에 `SEOUL` 집계 존재

### 최근 이벤트 조회

```bash
curl -s 'http://localhost:8080/api/traffic/events/recent?limit=20' \
  -H 'X-Trace-Id: manual-traffic-recent-001'
```

기대 결과:

- 배열 응답
- 입력한 `roadName: "강변북로"` 확인 가능

## N. 비동기 특성 이해 (중요)

이벤트 입력과 조회는 동기 즉시 일치가 아닐 수 있습니다.

- 이유: Command → Kafka → Query 반영이 비동기
- 실무 검증 시 1~3초 폴링을 권장

예시(간단 폴링):

```bash
for i in {1..10}; do
  curl -s 'http://localhost:8080/api/traffic/events/recent?limit=5' | grep '강변북로' && break
  sleep 1
done
```

## O. 입력값 규칙

- `region`: 필수 (권장 코드 사용)
- `roadName`: 필수 (한글 가능)
- `averageSpeedKph`: `0~200`
- `congestionLevel`: `1~5`
- `observedAt`: 생략 가능 (생략 시 서버 시각 기준 처리)

## P. 오류 응답 해석

공통 오류 포맷:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "...",
  "traceId": "...",
  "timestamp": "..."
}
```

자주 보는 코드:

- `VALIDATION_ERROR`: 입력값 형식 오류
- `BAD_REQUEST`: 요청 자체 문제
- `UNAUTHORIZED`: 토큰 문제
- `INTERNAL_SERVER_ERROR`: 내부 처리 실패

## Q. 시나리오별 검증 체크리스트

### 시나리오 1: 정상 수집 흐름

1. 회원가입 성공
2. 로그인 성공
3. 이벤트 입력 성공(`202`)
4. 요약 반영 확인
5. 최근 이벤트 반영 확인

### 시나리오 2: 검증 실패 흐름

1. `averageSpeedKph=-1`로 전송
2. `400 + VALIDATION_ERROR` 확인
3. `traceId` 존재 확인

### 시나리오 3: 인증 실패 흐름

1. 잘못된 Bearer 토큰으로 validate 호출
2. `401 + UNAUTHORIZED` 확인

## R. 운영 관측 포인트

- 프론트 헬스: `http://localhost:8084/actuator/health`
- 게이트웨이 헬스: `http://localhost:8080/actuator/health`
- 각 서비스 메트릭: `/actuator/prometheus`
- Prometheus: [http://localhost:9091](http://localhost:9091)
- Grafana: [http://localhost:3000](http://localhost:3000)

## S. 부하 테스트로 검증

```bash
BASE_URL=http://localhost:8080 ./performance/k6/run-all.sh
```

결과 확인:

- `performance/results/*.json`
- `performance/results/*.log`
- `docs/performance/K6_REPORT.md`

검증 포인트:

- 인증 시나리오 임계치 통과 여부
- 트래픽 시나리오 실패율/지연 시간

## T. 인수 테스트로 검증

```bash
./gradlew test
```

특히 아래 모듈 테스트가 핵심:

- auth 인수 테스트
- command 인수 테스트
- query 인수 테스트
- gateway 인수 테스트
- frontend 인수 테스트

## U. 한글 도로명 사용 가이드

이 앱은 UTF-8 문자열을 그대로 저장/전달하므로 한글 도로명 입력이 가능합니다.

권장 예시:

- `강변북로`
- `올림픽대로`
- `동부간선도로`
- `경부고속도로`

주의:

- `roadName` 빈 문자열은 검증 실패
- 너무 긴 문자열은 운영 정책상 별도 제한을 둘 것을 권장(현재는 NotBlank 중심)

## V. 로그 확인 방법

실행 로그 위치:

- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/auth-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-command-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-query-service.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/api-gateway.log`
- `/Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-frontend.log`

실시간 보기 예시:

```bash
tail -f /Users/ibm/IdeaProjects/infinity-traffic/.runtime/logs/traffic-query-service.log
```

## W. 중지/재기동

애플리케이션만 중지:

```bash
./scripts/local/stop-all.sh
```

재기동:

```bash
./scripts/local/start-all.sh
```

인프라까지 완전 중지:

```bash
docker compose -f /Users/ibm/IdeaProjects/infinity-traffic/infra/docker-compose.yml down
```

## X. 자주 발생하는 문제와 해결

### 1) `This site can’t be reached`

- 원인: `8084` 포트 미기동
- 해결: `./scripts/local/start-all.sh` 후 `./scripts/local/status-all.sh` 확인

### 2) 이벤트 넣었는데 조회에 안 보임

- 원인: 비동기 반영 지연
- 해결: 1~3초 간격으로 최근 이벤트 API 폴링

### 3) Kafka 관련 오류

- 원인: 브로커 준비 전 요청
- 해결: 인프라 재확인 후 command/query 재기동

## Y. 이 앱을 "잘 사용"한다는 기준

다음을 반복 가능하게 수행하면 이 앱을 잘 활용하는 상태입니다.

1. 자동 스크립트로 전체 스택 기동/중지 가능
2. UI와 API 양쪽에서 이벤트 입력 가능
3. 비동기 반영을 고려해 조회 검증 가능
4. 오류 응답(`code/message/traceId`)을 읽고 원인 판단 가능
5. k6와 인수 테스트로 회귀 검증 가능

## Z. 최종 운영 점검표

배포/데모/검증 전 마지막 체크:

1. `./scripts/local/status-all.sh` 전부 `UP`
2. `http://localhost:8084/dashboard` 접속 성공
3. 회원가입/로그인 성공
4. 한글 도로명 이벤트 1건 입력 성공
5. `summary`, `recent`에서 반영 확인
6. `./gradlew test` 통과
7. 필요 시 `k6` 실행 후 리포트 확인

---

이 문서는 "앱 사용/검증" 관점 중심입니다.
아키텍처 구현 상세는 `docs/IMPLEMENTATION_A_TO_Z.md`를 함께 참고하세요.
