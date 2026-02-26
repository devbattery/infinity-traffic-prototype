package com.devbattery.infinitytraffic.frontend.service

import com.devbattery.infinitytraffic.frontend.web.AuthTokenResponse
import com.devbattery.infinitytraffic.frontend.web.FrontendGatewayClient
import com.devbattery.infinitytraffic.frontend.web.FrontendUserSession
import com.devbattery.infinitytraffic.frontend.web.LoginRequest
import com.devbattery.infinitytraffic.frontend.web.TokenValidationResponse
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficRegionSummary
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.BDDMockito.given
import org.springframework.mock.web.MockHttpSession
import java.time.Instant

/**
 * TrafficFrontendService 단위 테스트다.
 */
@ExtendWith(MockitoExtension::class)
class TrafficFrontendServiceTest {

    @Mock
    private lateinit var frontendGatewayClient: FrontendGatewayClient

    @InjectMocks
    private lateinit var trafficFrontendService: TrafficFrontendService

    // 로그인 성공 시 세션에 인증 정보가 저장되는지 검증한다.
    @Test
    fun loginShouldPersistValidatedSession() {
        val expiresAt = Instant.parse("2026-12-31T00:00:00Z")
        val session = MockHttpSession()

        given(frontendGatewayClient.login(LoginRequest(username = "operator-1", password = "Password!1234")))
            .willReturn(
                AuthTokenResponse(
                    tokenType = "Bearer",
                    accessToken = "token-frontend-001",
                    expiresAt = expiresAt,
                ),
            )
        given(frontendGatewayClient.validate("token-frontend-001"))
            .willReturn(
                TokenValidationResponse(
                    valid = true,
                    username = "operator-1",
                    expiresAt = expiresAt,
                ),
            )

        val response = trafficFrontendService.login(
            request = LoginRequest(username = "operator-1", password = "Password!1234"),
            session = session,
        )

        assertThat(response.username).isEqualTo("operator-1")
        assertThat(response.expiresAt).isEqualTo(expiresAt)

        val saved = session.getAttribute(TrafficFrontendService.USER_SESSION_KEY) as FrontendUserSession
        assertThat(saved.username).isEqualTo("operator-1")
        assertThat(saved.accessToken).isEqualTo("token-frontend-001")
        assertThat(saved.expiresAt).isEqualTo(expiresAt)
    }

    // 대시보드 조회 시 region/limit 보정 규칙이 적용되는지 검증한다.
    @Test
    fun dashboardSnapshotShouldNormalizeRegionAndClampLimit() {
        val now = Instant.parse("2026-02-26T08:00:00Z")
        val summary =
            TrafficSummaryResponse(
                generatedAt = now,
                totalEvents = 7,
                regions =
                    listOf(
                        TrafficRegionSummary(
                            region = "SEOUL",
                            totalEvents = 7,
                            averageSpeedKph = 41.8,
                            latestCongestionLevel = 3,
                        ),
                    ),
            )
        val recentEvents =
            listOf(
                TrafficEventMessage(
                    eventId = "event-001",
                    traceId = "trace-001",
                    region = "SEOUL",
                    roadName = "강변북로",
                    averageSpeedKph = 40,
                    congestionLevel = 3,
                    observedAt = now,
                ),
            )

        given(frontendGatewayClient.summary("SEOUL")).willReturn(summary)
        given(frontendGatewayClient.recentEvents(100)).willReturn(recentEvents)

        val response = trafficFrontendService.dashboardSnapshot(region = " seoul ", limit = 999, session = MockHttpSession())

        assertThat(response.summary.totalEvents).isEqualTo(7)
        assertThat(response.recentEvents).hasSize(1)
        verify(frontendGatewayClient).summary("SEOUL")
        verify(frontendGatewayClient).recentEvents(100)
    }
}
