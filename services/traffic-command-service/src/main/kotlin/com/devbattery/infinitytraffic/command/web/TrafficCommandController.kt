package com.devbattery.infinitytraffic.command.web

import com.devbattery.infinitytraffic.command.service.TrafficEventIngestService
import com.devbattery.infinitytraffic.shared.contract.TrafficIngestResult
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.devbattery.infinitytraffic.shared.tracing.TraceIdGenerator
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 교통 이벤트 수집(Write Side) API를 제공한다.
 */
@RestController
@Validated
@RequestMapping("/api/v1/traffic")
class TrafficCommandController(
    private val trafficEventIngestService: TrafficEventIngestService,
) {

    // 이벤트 수집 요청을 받아 Kafka 발행까지 수행한다.
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun ingestEvent(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @Valid @RequestBody request: TrafficEventIngestRequest,
    ): TrafficIngestResult {
        return trafficEventIngestService.ingest(resolveTraceId(traceId), request)
    }

    // Trace ID가 없으면 신규 ID를 생성한다.
    private fun resolveTraceId(traceId: String?): String = traceId ?: TraceIdGenerator.create()
}
