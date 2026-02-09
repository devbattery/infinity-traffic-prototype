package com.devbattery.infinitytraffic.shared.contract

import java.time.Instant

/**
 * 커맨드 서비스에서 발행하고 쿼리 서비스가 소비하는 교통 이벤트 계약이다.
 */
data class TrafficEventMessage(
    val eventId: String,
    val traceId: String,
    val region: String,
    val roadName: String,
    val averageSpeedKph: Int,
    val congestionLevel: Int,
    val observedAt: Instant,
)

/**
 * 이벤트 수집 API의 결과를 게이트웨이와 클라이언트에 전달할 때 사용한다.
 */
data class TrafficIngestResult(
    val eventId: String,
    val status: String,
    val observedAt: Instant,
)

/**
 * 지역별 집계 결과를 표현한다.
 */
data class TrafficRegionSummary(
    val region: String,
    val totalEvents: Long,
    val averageSpeedKph: Double,
    val latestCongestionLevel: Int,
)

/**
 * 교통 요약 조회 API의 응답 모델이다.
 */
data class TrafficSummaryResponse(
    val generatedAt: Instant,
    val totalEvents: Long,
    val regions: List<TrafficRegionSummary>,
)

/**
 * 서비스 공통 에러 응답 포맷이다.
 */
data class ApiError(
    val code: String,
    val message: String,
    val traceId: String,
    val timestamp: Instant = Instant.now(),
)
