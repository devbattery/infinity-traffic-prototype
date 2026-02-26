package com.devbattery.infinitytraffic.frontend.service

import com.devbattery.infinitytraffic.frontend.web.PlatformOverviewResponse
import com.devbattery.infinitytraffic.frontend.web.PlatformValueStep
import org.springframework.stereotype.Service

/**
 * 사용자에게 서비스 목적과 운영 흐름을 명확히 전달하는 안내 정보를 제공한다.
 */
@Service
class PlatformGuideService {

    // 대시보드 목적/대상 사용자/핵심 흐름을 단일 응답으로 구성해 UI 혼란을 줄인다.
    fun overview(): PlatformOverviewResponse =
        PlatformOverviewResponse(
            productName = "Infinity Traffic Control Center",
            corePurpose = "실시간 교통 이벤트를 수집하고 지역별 혼잡 현황을 운영자가 즉시 판단할 수 있도록 지원하는 운영 대시보드",
            primaryUsers = "교통 관제 운영자, 데이터 운영팀, 플랫폼 SRE",
            keyCapabilities =
                listOf(
                    "이벤트 입력 즉시 Kafka 기반 파이프라인으로 전달",
                    "CQRS Query 모델을 통한 지역별 요약/최근 이벤트 조회",
                    "Spring Boot 메트릭을 Prometheus/Grafana로 통합 모니터링",
                ),
            valueFlow =
                listOf(
                    PlatformValueStep(
                        order = 1,
                        title = "이벤트 수집",
                        description = "관제 운영자가 지역, 도로, 속도, 혼잡도를 입력하면 Command 서비스가 이벤트를 발행합니다.",
                    ),
                    PlatformValueStep(
                        order = 2,
                        title = "실시간 집계",
                        description = "Query 서비스가 스트림 이벤트를 반영해 지역별 평균 속도/혼잡도를 갱신합니다.",
                    ),
                    PlatformValueStep(
                        order = 3,
                        title = "운영 판단",
                        description = "대시보드 KPI·요약·최근 이벤트를 기반으로 현장 대응 우선순위를 빠르게 결정합니다.",
                    ),
                    PlatformValueStep(
                        order = 4,
                        title = "시스템 모니터링",
                        description = "Grafana와 Prometheus에서 처리량·지연·서비스 헬스를 관찰해 장애를 조기 탐지합니다.",
                    ),
                ),
        )
}
