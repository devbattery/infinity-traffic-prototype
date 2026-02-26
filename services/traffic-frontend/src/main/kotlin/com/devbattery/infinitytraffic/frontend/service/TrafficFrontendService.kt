package com.devbattery.infinitytraffic.frontend.service

import com.devbattery.infinitytraffic.frontend.web.DashboardSnapshotResponse
import com.devbattery.infinitytraffic.frontend.web.FrontendGatewayClient
import com.devbattery.infinitytraffic.frontend.web.FrontendGatewayException
import com.devbattery.infinitytraffic.frontend.web.FrontendMessageResponse
import com.devbattery.infinitytraffic.frontend.web.FrontendUserSession
import com.devbattery.infinitytraffic.frontend.web.LoginRequest
import com.devbattery.infinitytraffic.frontend.web.LoginSuccessResponse
import com.devbattery.infinitytraffic.frontend.web.RegisterRequest
import com.devbattery.infinitytraffic.frontend.web.SessionSnapshotResponse
import com.devbattery.infinitytraffic.frontend.web.TrafficEventIngestRequest
import com.devbattery.infinitytraffic.frontend.web.TrafficEventIngestSuccessResponse
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 프론트엔드 API의 비즈니스 로직을 처리한다.
 */
@Service
class TrafficFrontendService(
    private val frontendGatewayClient: FrontendGatewayClient,
) {

    // 대시보드 카드/테이블에 필요한 데이터를 한 번에 반환한다.
    fun dashboardSnapshot(region: String?, limit: Int, session: HttpSession): DashboardSnapshotResponse {
        val safeRegion = normalizeRegion(region)
        val safeLimit = limit.coerceIn(5, 100)
        val userSession = resolveUserSession(session)

        return DashboardSnapshotResponse(
            generatedAt = Instant.now(),
            summary = frontendGatewayClient.summary(safeRegion),
            recentEvents = frontendGatewayClient.recentEvents(safeLimit),
            authenticated = userSession != null,
            username = userSession?.username,
            tokenExpiresAt = userSession?.expiresAt,
        )
    }

    // 현재 로그인 세션 스냅샷을 반환한다.
    fun sessionSnapshot(session: HttpSession): SessionSnapshotResponse {
        val userSession = resolveUserSession(session)
        return SessionSnapshotResponse(
            authenticated = userSession != null,
            username = userSession?.username,
            expiresAt = userSession?.expiresAt,
        )
    }

    // 회원가입 요청을 게이트웨이로 전달한다.
    fun register(request: RegisterRequest): FrontendMessageResponse {
        val response = frontendGatewayClient.register(normalized(request))
        return FrontendMessageResponse(message = "${response.username} 계정이 생성되었습니다.")
    }

    // 로그인 후 세션에 인증 정보를 저장한다.
    fun login(request: LoginRequest, session: HttpSession): LoginSuccessResponse {
        val normalizedRequest = normalized(request)
        val token = frontendGatewayClient.login(normalizedRequest)
        val validSession = createValidatedSession(
            requestUsername = normalizedRequest.username,
            accessToken = token.accessToken,
            fallbackExpiresAt = token.expiresAt,
        )

        session.setAttribute(USER_SESSION_KEY, validSession)

        return LoginSuccessResponse(
            message = "로그인에 성공했습니다.",
            username = validSession.username,
            expiresAt = validSession.expiresAt,
        )
    }

    // 로그인 세션을 종료한다.
    fun logout(session: HttpSession): FrontendMessageResponse {
        session.removeAttribute(USER_SESSION_KEY)
        return FrontendMessageResponse(message = "로그아웃되었습니다.")
    }

    // 교통 이벤트를 게이트웨이 수집 API로 전달한다.
    fun ingestTrafficEvent(request: TrafficEventIngestRequest): TrafficEventIngestSuccessResponse {
        val response = frontendGatewayClient.ingestTrafficEvent(
            request.copy(
                region = request.region.trim(),
                roadName = request.roadName.trim(),
            ),
        )

        return TrafficEventIngestSuccessResponse(
            message = "이벤트가 수집되었습니다.",
            eventId = response.eventId,
            status = response.status,
            observedAt = response.observedAt,
        )
    }

    // 지역 필터 문자열을 표준 값으로 보정한다.
    private fun normalizeRegion(region: String?): String? {
        val normalized = region?.trim()?.uppercase()
        return if (normalized.isNullOrBlank() || normalized == "ALL") {
            null
        } else {
            normalized
        }
    }

    // 세션 토큰을 검증해 유효한 경우에만 사용자 정보를 반환한다.
    private fun resolveUserSession(session: HttpSession): FrontendUserSession? {
        val saved = session.getAttribute(USER_SESSION_KEY) as? FrontendUserSession ?: return null
        return runCatching {
            createValidatedSession(
                requestUsername = saved.username,
                accessToken = saved.accessToken,
                fallbackExpiresAt = saved.expiresAt,
            )
        }.getOrElse {
            session.removeAttribute(USER_SESSION_KEY)
            null
        }
    }

    // 토큰 검증을 통과한 사용자 세션 객체를 생성한다.
    private fun createValidatedSession(requestUsername: String, accessToken: String, fallbackExpiresAt: Instant): FrontendUserSession {
        val validation = frontendGatewayClient.validate(accessToken)
        if (!validation.valid) {
            throw FrontendGatewayException(statusCode = 401, message = "토큰 검증에 실패했습니다.")
        }

        return FrontendUserSession(
            username = validation.username ?: requestUsername,
            accessToken = accessToken,
            expiresAt = validation.expiresAt ?: fallbackExpiresAt,
        )
    }

    // 회원가입 요청의 문자열 필드를 표준화한다.
    private fun normalized(request: RegisterRequest): RegisterRequest =
        request.copy(
            username = request.username.trim(),
            password = request.password,
        )

    // 로그인 요청의 문자열 필드를 표준화한다.
    private fun normalized(request: LoginRequest): LoginRequest =
        request.copy(
            username = request.username.trim(),
            password = request.password,
        )

    companion object {
        const val USER_SESSION_KEY: String = "trafficFrontendUserSession"
    }
}
