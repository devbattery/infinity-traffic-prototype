package com.devbattery.infinitytraffic.query.web

import com.devbattery.infinitytraffic.query.service.TrafficProjectionService
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 교통 조회(Read Side) API를 제공한다.
 */
@RestController
@Validated
@RequestMapping("/api/v1/traffic")
class TrafficQueryController(
    private val trafficProjectionService: TrafficProjectionService,
) {

    // 지역별 또는 전체 교통 요약을 조회한다.
    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) region: String?,
    ): TrafficSummaryResponse {
        return trafficProjectionService.summary(region)
    }

    // 최근 수집된 이벤트를 최신순으로 조회한다.
    @GetMapping("/events/recent")
    fun recentEvents(
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) limit: Int,
    ): List<TrafficEventMessage> {
        return trafficProjectionService.recent(limit)
    }
}
