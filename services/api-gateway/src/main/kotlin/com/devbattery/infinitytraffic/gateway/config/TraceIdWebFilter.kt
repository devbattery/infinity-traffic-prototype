package com.devbattery.infinitytraffic.gateway.config

import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.devbattery.infinitytraffic.shared.tracing.TraceIdGenerator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 요청 진입 시 Trace ID를 보정하고, 모든 응답에 동일한 Trace ID를 돌려준다.
 */
@Component
class TraceIdWebFilter : WebFilter {

    // 인입 헤더에 Trace ID가 없으면 생성해서 요청/응답 모두에 설정한다.
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val traceId = exchange.request.headers.getFirst(TraceHeaders.TRACE_ID) ?: TraceIdGenerator.create()
        val mutatedRequest = exchange.request.mutate().header(TraceHeaders.TRACE_ID, traceId).build()
        val mutatedExchange = exchange.mutate().request(mutatedRequest).build()

        mutatedExchange.response.headers.set(TraceHeaders.TRACE_ID, traceId)
        return chain.filter(mutatedExchange)
    }
}
