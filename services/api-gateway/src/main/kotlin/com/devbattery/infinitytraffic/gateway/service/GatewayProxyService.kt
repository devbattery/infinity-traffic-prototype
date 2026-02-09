package com.devbattery.infinitytraffic.gateway.service

import com.devbattery.infinitytraffic.gateway.config.GatewayServiceProperties
import com.devbattery.infinitytraffic.gateway.web.LoginRequest
import com.devbattery.infinitytraffic.gateway.web.RegisterRequest
import com.devbattery.infinitytraffic.gateway.web.TrafficEventIngestRequest
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * 게이트웨이에서 각 마이크로서비스로 요청을 전달하는 프록시 계층이다.
 */
@Service
class GatewayProxyService(
    private val webClientBuilder: WebClient.Builder,
    private val properties: GatewayServiceProperties,
) {

    // 회원가입 요청을 인증 서비스로 전달한다.
    fun register(traceId: String, request: RegisterRequest): Mono<ResponseEntity<String>> =
        forwardPost(properties.auth, "/api/v1/auth/register", traceId, request)

    // 로그인 요청을 인증 서비스로 전달한다.
    fun login(traceId: String, request: LoginRequest): Mono<ResponseEntity<String>> =
        forwardPost(properties.auth, "/api/v1/auth/login", traceId, request)

    // 토큰 검증 요청을 인증 서비스로 전달한다.
    fun validate(traceId: String, authorization: String): Mono<ResponseEntity<String>> =
        forwardGet(
            baseUrl = properties.auth,
            path = "/api/v1/auth/validate",
            traceId = traceId,
            queryParams = emptyMap(),
            authorization = authorization,
        )

    // 교통 이벤트 수집 요청을 커맨드 서비스로 전달한다.
    fun ingestTrafficEvent(traceId: String, request: TrafficEventIngestRequest): Mono<ResponseEntity<String>> =
        forwardPost(properties.command, "/api/v1/traffic/events", traceId, request)

    // 교통 집계 조회 요청을 쿼리 서비스로 전달한다.
    fun getTrafficSummary(traceId: String, region: String?): Mono<ResponseEntity<String>> =
        forwardGet(
            baseUrl = properties.query,
            path = "/api/v1/traffic/summary",
            traceId = traceId,
            queryParams = mapOf("region" to region),
        )

    // 최근 이벤트 조회 요청을 쿼리 서비스로 전달한다.
    fun getRecentTrafficEvents(traceId: String, limit: Int): Mono<ResponseEntity<String>> =
        forwardGet(
            baseUrl = properties.query,
            path = "/api/v1/traffic/events/recent",
            traceId = traceId,
            queryParams = mapOf("limit" to limit.toString()),
        )

    // POST 요청을 공통 포맷으로 다운스트림 서비스에 전달한다.
    private fun forwardPost(
        baseUrl: String,
        path: String,
        traceId: String,
        payload: Any,
    ): Mono<ResponseEntity<String>> {
        return webClientBuilder
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .header(TraceHeaders.TRACE_ID, traceId)
            .bodyValue(payload)
            .exchangeToMono(::toResponseEntity)
    }

    // GET 요청을 공통 포맷으로 다운스트림 서비스에 전달한다.
    private fun forwardGet(
        baseUrl: String,
        path: String,
        traceId: String,
        queryParams: Map<String, String?>,
        authorization: String? = null,
    ): Mono<ResponseEntity<String>> {
        return webClientBuilder
            .baseUrl(baseUrl)
            .build()
            .get()
            .uri { uriBuilder ->
                queryParams
                    .entries
                    .fold(uriBuilder.path(path)) { builder, entry ->
                        val value = entry.value
                        if (value.isNullOrBlank()) {
                            builder
                        } else {
                            builder.queryParam(entry.key, value)
                        }
                    }
                    .build()
            }
            .headers { headers ->
                headers.set(TraceHeaders.TRACE_ID, traceId)
                authorization?.let { headers.set(HttpHeaders.AUTHORIZATION, it) }
            }
            .exchangeToMono(::toResponseEntity)
    }

    // 다운스트림 상태코드/본문을 보존해서 게이트웨이 응답으로 변환한다.
    private fun toResponseEntity(clientResponse: ClientResponse): Mono<ResponseEntity<String>> {
        return clientResponse
            .bodyToMono(String::class.java)
            .defaultIfEmpty("")
            .map { body ->
                ResponseEntity.status(clientResponse.statusCode()).body(body)
            }
    }
}
