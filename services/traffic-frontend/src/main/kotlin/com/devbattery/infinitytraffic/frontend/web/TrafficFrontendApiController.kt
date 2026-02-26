package com.devbattery.infinitytraffic.frontend.web

import com.devbattery.infinitytraffic.frontend.service.FrontendMonitoringService
import com.devbattery.infinitytraffic.frontend.service.PlatformGuideService
import com.devbattery.infinitytraffic.frontend.service.TrafficFrontendService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * React SPA가 사용하는 JSON API를 제공한다.
 */
@RestController
@RequestMapping("/ui/api")
class TrafficFrontendApiController(
    private val trafficFrontendService: TrafficFrontendService,
    private val platformGuideService: PlatformGuideService,
    private val frontendMonitoringService: FrontendMonitoringService,
) {

    // 대시보드 카드/테이블에 필요한 데이터를 한 번에 반환한다.
    @GetMapping("/dashboard")
    fun dashboardSnapshot(
        @RequestParam(required = false) region: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        session: HttpSession,
    ): DashboardSnapshotResponse =
        trafficFrontendService.dashboardSnapshot(region = region, limit = limit, session = session)

    // 사용자가 서비스 목적/사용 흐름을 이해할 수 있도록 안내 정보를 반환한다.
    @GetMapping("/platform/overview")
    fun platformOverview(): PlatformOverviewResponse =
        platformGuideService.overview()

    // Grafana/Prometheus 링크와 서비스 헬스 상태를 한 번에 반환한다.
    @GetMapping("/monitoring/snapshot")
    fun monitoringSnapshot(): MonitoringSnapshotResponse =
        frontendMonitoringService.snapshot()

    // 현재 로그인 상태를 조회한다.
    @GetMapping("/session")
    fun session(session: HttpSession): SessionSnapshotResponse =
        trafficFrontendService.sessionSnapshot(session)

    // 회원가입 요청을 처리한다.
    @PostMapping("/auth/register")
    fun register(@Valid @RequestBody request: RegisterRequest): FrontendMessageResponse =
        trafficFrontendService.register(request)

    // 로그인 요청을 처리하고 세션을 생성한다.
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest, session: HttpSession): LoginSuccessResponse =
        trafficFrontendService.login(request = request, session = session)

    // 로그인 세션을 종료한다.
    @PostMapping("/auth/logout")
    fun logout(session: HttpSession): FrontendMessageResponse =
        trafficFrontendService.logout(session)

    // 교통 이벤트 수집 요청을 처리한다.
    @PostMapping("/traffic/events")
    fun ingestTrafficEvent(@Valid @RequestBody request: TrafficEventIngestRequest): TrafficEventIngestSuccessResponse =
        trafficFrontendService.ingestTrafficEvent(request)
}
