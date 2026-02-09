package com.devbattery.infinitytraffic.command.web

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/**
 * 교통 이벤트 수집 요청 모델이다.
 */
data class TrafficEventIngestRequest(
    @field:NotBlank(message = "region은 필수입니다.")
    val region: String,
    @field:NotBlank(message = "roadName은 필수입니다.")
    val roadName: String,
    @field:Min(value = 0, message = "averageSpeedKph는 0 이상이어야 합니다.")
    @field:Max(value = 200, message = "averageSpeedKph는 200 이하여야 합니다.")
    val averageSpeedKph: Int,
    @field:Min(value = 1, message = "congestionLevel은 1 이상이어야 합니다.")
    @field:Max(value = 5, message = "congestionLevel은 5 이하여야 합니다.")
    val congestionLevel: Int,
    val observedAt: Instant? = null,
)
