package com.devbattery.infinitytraffic.gateway.acceptance

import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * 게이트웨이 인수 테스트를 수행한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayAcceptanceTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    companion object {
        private val authMockServer = MockWebServer()
        private val commandMockServer = MockWebServer()
        private val queryMockServer = MockWebServer()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            authMockServer.start()
            commandMockServer.start()
            queryMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            authMockServer.shutdown()
            commandMockServer.shutdown()
            queryMockServer.shutdown()
        }

        // 게이트웨이가 호출할 다운스트림 주소를 MockWebServer로 주입한다.
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("gateway.services.auth") { authMockServer.url("/").toString().removeSuffix("/") }
            registry.add("gateway.services.command") { commandMockServer.url("/").toString().removeSuffix("/") }
            registry.add("gateway.services.query") { queryMockServer.url("/").toString().removeSuffix("/") }
        }
    }

    // 로그인 API가 인증 서비스로 올바르게 라우팅되고 Trace ID를 전달하는지 검증한다.
    @Test
    fun loginShouldRouteToAuthServiceWithTraceId() {
        authMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "tokenType": "Bearer",
                      "accessToken": "token-123",
                      "expiresAt": "2026-02-09T00:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val traceId = "trace-gateway-acceptance-001"
        val requestBody =
            """
            {
              "username": "traffic-user",
              "password": "Password!123"
            }
            """.trimIndent()

        val response = post(
            path = "/api/auth/login",
            body = requestBody,
            headers = mapOf(TraceHeaders.TRACE_ID to traceId),
        )

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.headers().firstValue(TraceHeaders.TRACE_ID).orElse("")).isEqualTo(traceId)

        val responseJson: JsonNode = objectMapper.readTree(response.body())
        assertThat(responseJson["accessToken"].asText()).isEqualTo("token-123")

        val forwardedRequest = authMockServer.takeRequest()
        assertThat(forwardedRequest.path).isEqualTo("/api/v1/auth/login")
        assertThat(forwardedRequest.getHeader(TraceHeaders.TRACE_ID)).isEqualTo(traceId)
    }

    // 교통 조회 API가 쿼리 서비스로 정확히 라우팅되는지 검증한다.
    @Test
    fun summaryShouldRouteToQueryService() {
        queryMockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "generatedAt": "2026-02-09T00:00:00Z",
                      "totalEvents": 3,
                      "regions": [
                        {
                          "region": "SEOUL",
                          "totalEvents": 3,
                          "averageSpeedKph": 44.3,
                          "latestCongestionLevel": 4
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val response = get("/api/traffic/summary?region=SEOUL")
        assertThat(response.statusCode()).isEqualTo(200)

        val responseJson: JsonNode = objectMapper.readTree(response.body())
        assertThat(responseJson["regions"][0]["region"].asText()).isEqualTo("SEOUL")

        val forwardedRequest = queryMockServer.takeRequest()
        assertThat(forwardedRequest.path).isEqualTo("/api/v1/traffic/summary?region=SEOUL")
    }

    // JSON POST 요청을 전송한다.
    private fun post(path: String, body: String, headers: Map<String, String>): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")

        headers.forEach { (name, value) -> builder.header(name, value) }

        val request = builder
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    // GET 요청을 전송한다.
    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
