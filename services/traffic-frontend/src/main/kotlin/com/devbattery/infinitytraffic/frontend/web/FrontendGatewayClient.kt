package com.devbattery.infinitytraffic.frontend.web

import com.devbattery.infinitytraffic.frontend.config.FrontendGatewayProperties
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

/**
 * 타임리프 프론트엔드에서 API 게이트웨이를 호출하는 어댑터다.
 */
@Service
class FrontendGatewayClient(
    restClientBuilder: RestClient.Builder,
    private val properties: FrontendGatewayProperties,
    private val objectMapper: ObjectMapper,
) {

    private val restClient: RestClient = restClientBuilder.baseUrl(properties.baseUrl).build()
    private val recentEventsType = objectMapper.typeFactory.constructCollectionType(List::class.java, TrafficEventMessage::class.java)

    // 회원가입 요청을 게이트웨이로 전송한다.
    fun register(request: RegisterRequest): RegisterResponse =
        execute(
            method = HttpMethod.POST,
            path = "/api/auth/register",
            payload = request,
        ) { body -> objectMapper.readValue(body, RegisterResponse::class.java) }

    // 로그인 요청을 게이트웨이로 전송한다.
    fun login(request: LoginRequest): AuthTokenResponse =
        execute(
            method = HttpMethod.POST,
            path = "/api/auth/login",
            payload = request,
        ) { body -> objectMapper.readValue(body, AuthTokenResponse::class.java) }

    // 액세스 토큰을 게이트웨이를 통해 검증한다.
    fun validate(accessToken: String): TokenValidationResponse =
        execute(
            method = HttpMethod.GET,
            path = "/api/auth/validate",
            authorization = "Bearer $accessToken",
        ) { body -> objectMapper.readValue(body, TokenValidationResponse::class.java) }

    // 교통 이벤트를 수집 API로 전송한다.
    fun ingestTrafficEvent(request: TrafficEventIngestRequest): TrafficIngestResponse =
        execute(
            method = HttpMethod.POST,
            path = "/api/traffic/events",
            payload = request,
        ) { body -> objectMapper.readValue(body, TrafficIngestResponse::class.java) }

    // 교통 요약 데이터를 게이트웨이에서 조회한다.
    fun summary(region: String?): TrafficSummaryResponse =
        execute(
            method = HttpMethod.GET,
            path = "/api/traffic/summary",
            queryParams = mapOf("region" to region),
        ) { body -> objectMapper.readValue(body, TrafficSummaryResponse::class.java) }

    // 최근 이벤트 목록을 게이트웨이에서 조회한다.
    fun recentEvents(limit: Int): List<TrafficEventMessage> =
        execute(
            method = HttpMethod.GET,
            path = "/api/traffic/events/recent",
            queryParams = mapOf("limit" to limit.toString()),
        ) { body ->
            if (body.isBlank()) {
                emptyList()
            } else {
                objectMapper.readValue(body, recentEventsType)
            }
        }

    // 공통 HTTP 호출/에러 변환/JSON 파싱을 수행한다.
    private fun <T> execute(
        method: HttpMethod,
        path: String,
        payload: Any? = null,
        queryParams: Map<String, String?> = emptyMap(),
        authorization: String? = null,
        parser: (String) -> T,
    ): T {
        val requestSpec = restClient
            .method(method)
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
                headers.contentType = MediaType.APPLICATION_JSON
                authorization?.let { headers.set(HttpHeaders.AUTHORIZATION, it) }
            }

        return if (payload != null) {
            requestSpec
                .body(payload)
                .exchange { _, response -> toResult(response.statusCode.is2xxSuccessful, response.statusCode.value(), response.body.bufferedReader().use { it.readText() }, parser) }
        } else {
            requestSpec
                .exchange { _, response -> toResult(response.statusCode.is2xxSuccessful, response.statusCode.value(), response.body.bufferedReader().use { it.readText() }, parser) }
        }
    }

    // HTTP 응답 바디를 성공/실패 규칙에 따라 도메인 값으로 변환한다.
    private fun <T> toResult(isSuccess: Boolean, statusCode: Int, rawBody: String, parser: (String) -> T): T {
        if (isSuccess) {
            return parser(rawBody)
        }

        throw FrontendGatewayException(
            statusCode = statusCode,
            message = extractErrorMessage(rawBody),
        )
    }

    // 다운스트림 에러 본문에서 사람이 읽을 수 있는 메시지를 추출한다.
    private fun extractErrorMessage(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "게이트웨이에서 빈 오류 응답을 반환했습니다."
        }

        return runCatching {
            val node = objectMapper.readTree(rawBody)
            node.path("message").asText(rawBody)
        }.getOrElse { rawBody }
    }
}

/**
 * 게이트웨이 호출 실패를 프론트엔드 도메인 예외로 표현한다.
 */
class FrontendGatewayException(
    val statusCode: Int,
    override val message: String,
    val timestamp: Instant = Instant.now(),
) : RuntimeException(message)
