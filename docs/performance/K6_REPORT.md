# k6 부하 테스트 리포트

## 실행 일시

- 실행일: 2026-02-09
- 실행 스크립트: `performance/k6/run-all.sh`
- 대상 URL: `http://localhost:8080`

## 시나리오 1) 인증 플로우 (`auth-flow.js`)

### 부하 모델

- Executor: `ramping-vus`
- Stage:
  - 20초 동안 30 VU까지 램프업
  - 30초 동안 30 VU 유지
  - 20초 동안 0 VU로 램프다운

### 임계치 결과

- `http_req_failed < 1%`: 통과 (`0.00%`)
- `http_req_duration p(95) < 1200ms`: 통과 (`996.70ms`)
- `http_req_duration p(99) < 2000ms`: 통과 (`1.17s`)

### 핵심 수치

- 총 요청 수: `3,006`
- 총 반복 수: `1,002`
- 평균 응답시간: `452.56ms`
- P95 응답시간: `996.70ms`
- 최대 응답시간: `2.17s`
- 체크 성공률: `100%` (`5,010/5,010`)

## 시나리오 2) 이벤트 수집 + 조회 (`traffic-flow.js`)

### 부하 모델

- Executor: `ramping-vus`
- Stage:
  - 20초 동안 40 VU까지 램프업
  - 40초 동안 40 VU 유지
  - 20초 동안 0 VU로 램프다운

### 임계치 결과

- `http_req_failed < 2%`: 통과 (`0.38%`)
- `http_req_duration p(95) < 1800ms`: 통과 (`69.15ms`)
- `http_req_duration p(99) < 2800ms`: 통과 (`99.66ms`)

### 핵심 수치

- 총 요청 수: `31,728`
- 총 반복 수: `10,576`
- 평균 응답시간: `26.15ms`
- P95 응답시간: `69.15ms`
- 최대 응답시간: `935.63ms`
- 체크 성공률: `99.61%` (`31,606/31,728`)

### 실패 체크 상세

- `ingest status is 202`: `45` 실패
- `summary status is 200`: `26` 실패
- `recent status is 200`: `51` 실패

초기 구간에서 Kafka 토픽 리더 선출 타이밍(`LEADER_NOT_AVAILABLE` 경고)과 겹치며 일부 요청이 실패한 것으로 관찰됐고, 전체 임계치는 통과했습니다.

## 산출물

- 요약 JSON:
  - `performance/results/auth_flow_20260209_231349.json`
  - `performance/results/traffic_flow_20260209_231349.json`
- 원본 로그:
  - `performance/results/auth_flow_20260209_231349.log`
  - `performance/results/traffic_flow_20260209_231349.log`

## 재실행 방법

```bash
BASE_URL=http://localhost:8080 ./performance/k6/run-all.sh
```
