package com.devbattery.infinitytraffic.frontend.web

import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * 게이트웨이 회원가입 API 요청 모델이다.
 */
data class RegisterRequest(
    @field:NotBlank(message = "아이디를 입력해 주세요.")
    val username: String,
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    val password: String,
)

/**
 * 게이트웨이 로그인 API 요청 모델이다.
 */
data class LoginRequest(
    @field:NotBlank(message = "아이디를 입력해 주세요.")
    val username: String,
    @field:NotBlank(message = "비밀번호를 입력해 주세요.")
    val password: String,
)

/**
 * 게이트웨이 이벤트 수집 API 요청 모델이다.
 */
data class TrafficEventIngestRequest(
    @field:NotBlank(message = "지역을 선택해 주세요.")
    val region: String,
    @field:NotBlank(message = "도로명을 입력해 주세요.")
    val roadName: String,
    @field:Min(value = 0, message = "평균 속도는 0 이상이어야 합니다.")
    @field:Max(value = 200, message = "평균 속도는 200 이하이어야 합니다.")
    val averageSpeedKph: Int,
    @field:Min(value = 1, message = "혼잡도는 1 이상이어야 합니다.")
    @field:Max(value = 5, message = "혼잡도는 5 이하여야 합니다.")
    val congestionLevel: Int,
    val observedAt: Instant? = null,
)

/**
 * 회원가입 응답 모델이다.
 */
data class RegisterResponse(
    val username: String,
    val createdAt: Instant,
)

/**
 * 토큰 발급 응답 모델이다.
 */
data class AuthTokenResponse(
    val tokenType: String,
    val accessToken: String,
    val expiresAt: Instant,
)

/**
 * 토큰 검증 응답 모델이다.
 */
data class TokenValidationResponse(
    val valid: Boolean,
    val username: String?,
    val expiresAt: Instant?,
)

/**
 * 이벤트 수집 성공 응답 모델이다.
 */
data class TrafficIngestResponse(
    val eventId: String,
    val status: String,
    val observedAt: Instant,
)

/**
 * 화면 세션에 저장할 사용자 인증 상태다.
 */
data class FrontendUserSession(
    val username: String,
    val accessToken: String,
    val expiresAt: Instant,
)

/**
 * 대시보드 스냅샷 응답 모델이다.
 */
data class DashboardSnapshotResponse(
    val generatedAt: Instant,
    val summary: TrafficSummaryResponse,
    val recentEvents: List<TrafficEventMessage>,
    val authenticated: Boolean,
    val username: String?,
    val tokenExpiresAt: Instant?,
)

/**
 * 현재 세션 스냅샷 응답 모델이다.
 */
data class SessionSnapshotResponse(
    val authenticated: Boolean,
    val username: String?,
    val expiresAt: Instant?,
)

/**
 * 단순 메시지 응답 모델이다.
 */
data class FrontendMessageResponse(
    val message: String,
    val timestamp: Instant = Instant.now(),
)

/**
 * 로그인 성공 응답 모델이다.
 */
data class LoginSuccessResponse(
    val message: String,
    val username: String,
    val expiresAt: Instant,
)

/**
 * 이벤트 수집 성공 응답 모델이다.
 */
data class TrafficEventIngestSuccessResponse(
    val message: String,
    val eventId: String,
    val status: String,
    val observedAt: Instant,
)

/**
 * 프론트엔드 JSON API 오류 응답 모델이다.
 */
data class FrontendApiError(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
