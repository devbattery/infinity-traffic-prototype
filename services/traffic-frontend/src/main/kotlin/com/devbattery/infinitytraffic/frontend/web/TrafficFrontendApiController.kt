package com.devbattery.infinitytraffic.frontend.web

import jakarta.servlet.http.HttpSession
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 대시보드 Ajax 갱신을 위한 JSON API를 제공한다.
 */
@RestController
@RequestMapping("/ui/api")
class TrafficFrontendApiController(
    private val frontendGatewayClient: FrontendGatewayClient,
) {

    // 대시보드 카드/테이블에 필요한 데이터를 한 번에 반환한다.
    @GetMapping("/dashboard")
    fun dashboardSnapshot(
        @RequestParam(required = false) region: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        session: HttpSession,
    ): DashboardSnapshotResponse {
        val safeRegion = normalizeRegion(region)
        val safeLimit = limit.coerceIn(5, 100)
        val userSession = sessionSnapshot(session)

        return DashboardSnapshotResponse(
            generatedAt = Instant.now(),
            summary = frontendGatewayClient.summary(safeRegion),
            recentEvents = frontendGatewayClient.recentEvents(safeLimit),
            authenticated = userSession != null,
            username = userSession?.username,
        )
    }

    // 현재 로그인 상태를 단순 조회한다.
    @GetMapping("/session")
    fun session(session: HttpSession): Map<String, Any?> {
        val userSession = sessionSnapshot(session)
        return mapOf(
            "authenticated" to (userSession != null),
            "username" to userSession?.username,
            "expiresAt" to userSession?.expiresAt,
        )
    }

    // 세션 토큰이 유효할 때만 사용자 정보를 반환한다.
    private fun sessionSnapshot(session: HttpSession): FrontendUserSession? {
        val saved = session.getAttribute(TrafficFrontendPageController.USER_SESSION_KEY) as? FrontendUserSession ?: return null
        return runCatching {
            val validation = frontendGatewayClient.validate(saved.accessToken)
            if (!validation.valid) {
                session.removeAttribute(TrafficFrontendPageController.USER_SESSION_KEY)
                null
            } else {
                val username = validation.username ?: saved.username
                FrontendUserSession(username = username, accessToken = saved.accessToken, expiresAt = saved.expiresAt)
            }
        }.getOrElse {
            session.removeAttribute(TrafficFrontendPageController.USER_SESSION_KEY)
            null
        }
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
}
