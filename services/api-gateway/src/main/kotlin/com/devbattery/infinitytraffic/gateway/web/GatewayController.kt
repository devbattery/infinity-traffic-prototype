package com.devbattery.infinitytraffic.gateway.web

import com.devbattery.infinitytraffic.gateway.service.GatewayProxyService
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.devbattery.infinitytraffic.shared.tracing.TraceIdGenerator
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * 외부 클라이언트 요청을 도메인별 마이크로서비스로 라우팅하는 진입 컨트롤러다.
 */
@RestController
@Validated
@RequestMapping("/api")
class GatewayController(
    private val gatewayProxyService: GatewayProxyService,
) {

    // 회원가입 요청을 인증 서비스로 전달한다.
    @PostMapping("/auth/register")
    fun register(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @Valid @RequestBody request: RegisterRequest,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.register(resolveTraceId(traceId), request)
    }

    // 로그인 요청을 인증 서비스로 전달한다.
    @PostMapping("/auth/login")
    fun login(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @Valid @RequestBody request: LoginRequest,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.login(resolveTraceId(traceId), request)
    }

    // 액세스 토큰 검증 요청을 인증 서비스로 전달한다.
    @GetMapping("/auth/validate")
    fun validate(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.validate(resolveTraceId(traceId), authorization)
    }

    // 교통 이벤트 수집 요청을 커맨드 서비스로 전달한다.
    @PostMapping("/traffic/events")
    fun ingestTrafficEvent(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @Valid @RequestBody request: TrafficEventIngestRequest,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.ingestTrafficEvent(resolveTraceId(traceId), request)
    }

    // 교통 요약 조회 요청을 쿼리 서비스로 전달한다.
    @GetMapping("/traffic/summary")
    fun trafficSummary(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @RequestParam(required = false) region: String?,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.getTrafficSummary(resolveTraceId(traceId), region)
    }

    // 최근 이벤트 조회 요청을 쿼리 서비스로 전달한다.
    @GetMapping("/traffic/events/recent")
    fun recentTrafficEvents(
        @RequestHeader(TraceHeaders.TRACE_ID, required = false) traceId: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(200) limit: Int,
    ): Mono<ResponseEntity<String>> {
        return gatewayProxyService.getRecentTrafficEvents(resolveTraceId(traceId), limit)
    }

    // 인입 헤더의 Trace ID가 없으면 신규 Trace ID를 생성한다.
    private fun resolveTraceId(traceId: String?): String = traceId ?: TraceIdGenerator.create()
}
