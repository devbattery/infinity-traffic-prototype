# 인수 테스트

서비스별 블랙박스 관점 인수 테스트를 작성했습니다.

## 1) 인증 플로우

- 파일: `services/auth-service/src/test/kotlin/com/devbattery/infinitytraffic/auth/acceptance/AuthAcceptanceTest.kt`
- 시나리오:
  1. 회원가입
  2. 로그인(토큰 발급)
  3. 토큰 검증

## 2) 커맨드 서비스 이벤트 발행

- 파일: `services/traffic-command-service/src/test/kotlin/com/devbattery/infinitytraffic/command/acceptance/TrafficCommandAcceptanceTest.kt`
- 시나리오:
  1. 이벤트 수집 API 호출
  2. Embedded Kafka 토픽에서 메시지 수신 확인
  3. Trace ID/핵심 필드 검증

## 3) 쿼리 서비스 소비/조회

- 파일: `services/traffic-query-service/src/test/kotlin/com/devbattery/infinitytraffic/query/acceptance/TrafficQueryAcceptanceTest.kt`
- 시나리오:
  1. Embedded Kafka에 이벤트 발행
  2. 쿼리 서비스가 이벤트를 소비해 투영
  3. 요약/최근 이벤트 조회 API 검증

## 4) 게이트웨이 라우팅

- 파일: `services/api-gateway/src/test/kotlin/com/devbattery/infinitytraffic/gateway/acceptance/GatewayAcceptanceTest.kt`
- 시나리오:
  1. MockWebServer로 다운스트림(Auth/Query) 대역 구성
  2. 게이트웨이 API 호출
  3. 다운스트림 경로/Trace ID 전달 검증
