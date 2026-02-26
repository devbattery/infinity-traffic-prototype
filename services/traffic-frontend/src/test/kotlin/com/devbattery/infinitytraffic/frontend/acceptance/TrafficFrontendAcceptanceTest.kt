package com.devbattery.infinitytraffic.frontend.acceptance

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
import java.util.concurrent.TimeUnit

/**
 * React SPA 기반 traffic-frontend 인수 테스트를 수행한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrafficFrontendAcceptanceTest {

    @LocalServerPort
    private var port: Int = 0

    private val client: HttpClient = HttpClient.newBuilder().build()
    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    companion object {
        private val gatewayMockServer = MockWebServer()

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            gatewayMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            gatewayMockServer.shutdown()
        }

        // 프론트엔드가 호출할 게이트웨이 주소를 MockWebServer로 대체한다.
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("frontend.gateway.base-url") { gatewayMockServer.url("/").toString().removeSuffix("/") }
        }
    }

    // /dashboard 경로가 React SPA 셸(index.html)을 반환하는지 검증한다.
    @Test
    fun dashboardShouldServeReactSpaShell() {
        val response = get(path = "/dashboard")

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("Infinity Traffic Control Center")
        assertThat(response.body()).contains("<div id=\"app\"></div>")
    }

    // 대시보드 스냅샷 API가 게이트웨이 요약/최근 이벤트를 통합해서 반환하는지 검증한다.
    @Test
    fun dashboardSnapshotShouldReturnGatewayData() {
        installDispatcher(totalEvents = 12)

        val response = get(path = "/ui/api/dashboard?region=SEOUL&limit=20")

        assertThat(response.statusCode()).isEqualTo(200)
        val payload = objectMapper.readTree(response.body())
        assertThat(payload["summary"]["totalEvents"].asLong()).isEqualTo(12)
        assertThat(payload["recentEvents"].size()).isEqualTo(1)

        val requestPaths = collectRequestPaths(4)
        assertThat(requestPaths.any { it.startsWith("/api/traffic/summary") }).isTrue()
        assertThat(requestPaths.any { it.startsWith("/api/traffic/events/recent") }).isTrue()
    }

    // 로그인 API 호출 후 세션 API가 인증 사용자 정보를 반환하는지 검증한다.
    @Test
    fun loginThenSessionShouldReturnAuthenticatedUser() {
        installDispatcher(totalEvents = 25)

        val loginResponse =
            postJson(
                path = "/ui/api/auth/login",
                body =
                    """
                    {
                      "username": "operator-1",
                      "password": "Password!1234"
                    }
                    """.trimIndent(),
            )

        assertThat(loginResponse.statusCode()).isEqualTo(200)
        val loginJson = objectMapper.readTree(loginResponse.body())
        assertThat(loginJson["username"].asText()).isEqualTo("operator-1")

        val sessionCookie = loginResponse.headers().firstValue("set-cookie").orElse("").substringBefore(';')
        assertThat(sessionCookie).startsWith("JSESSIONID=")

        val sessionResponse = get(path = "/ui/api/session", headers = mapOf("Cookie" to sessionCookie))
        assertThat(sessionResponse.statusCode()).isEqualTo(200)

        val sessionJson = objectMapper.readTree(sessionResponse.body())
        assertThat(sessionJson["authenticated"].asBoolean()).isTrue()
        assertThat(sessionJson["username"].asText()).isEqualTo("operator-1")

        val requestPaths = collectRequestPaths(6)
        assertThat(requestPaths).contains("/api/auth/login")
        assertThat(requestPaths).contains("/api/auth/validate")
    }

    // 이벤트 수집 API가 게이트웨이 호출 결과를 메시지와 함께 반환하는지 검증한다.
    @Test
    fun ingestShouldProxyToGatewayAndReturnMessage() {
        installDispatcher(totalEvents = 10)

        val response =
            postJson(
                path = "/ui/api/traffic/events",
                body =
                    """
                    {
                      "region": "SEOUL",
                      "roadName": "강변북로",
                      "averageSpeedKph": 42,
                      "congestionLevel": 3,
                      "observedAt": null
                    }
                    """.trimIndent(),
            )

        assertThat(response.statusCode()).isEqualTo(200)
        val payload = objectMapper.readTree(response.body())
        assertThat(payload["eventId"].asText()).isEqualTo("event-001")
        assertThat(payload["message"].asText()).contains("이벤트가 수집되었습니다")

        val requestPaths = collectRequestPaths(2)
        assertThat(requestPaths).contains("/api/traffic/events")
    }

    // 요청 경로 기준으로 목 응답을 반환하도록 Dispatcher를 설정한다.
    private fun installDispatcher(totalEvents: Long) {
        gatewayMockServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path ?: ""
                    return when {
                        path == "/api/auth/login" -> jsonResponse(loginResponseBody())
                        path == "/api/auth/validate" -> jsonResponse(validateResponseBody())
                        path.startsWith("/api/traffic/summary") -> jsonResponse(summaryResponseBody(totalEvents))
                        path.startsWith("/api/traffic/events/recent") -> jsonResponse(recentResponseBody())
                        path == "/api/auth/register" -> jsonResponse(registerResponseBody())
                        path == "/api/traffic/events" -> jsonResponse(ingestResponseBody(), 202)
                        else -> jsonResponse("{\"message\":\"not found\"}", 404)
                    }
                }
            }
    }

    // 수집된 요청 경로를 지정한 개수만큼 읽어 리스트로 반환한다.
    private fun collectRequestPaths(maxCount: Int): List<String> {
        val paths = mutableListOf<String>()
        repeat(maxCount) {
            val request = gatewayMockServer.takeRequest(200, TimeUnit.MILLISECONDS) ?: return@repeat
            request.path?.let { paths.add(it) }
        }
        return paths
    }

    // JSON 응답 객체를 생성한다.
    private fun jsonResponse(body: String, status: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(status)
            .addHeader("Content-Type", "application/json")
            .setBody(body)

    // 로그인 응답 바디를 반환한다.
    private fun loginResponseBody(): String =
        """
        {
          "tokenType": "Bearer",
          "accessToken": "token-frontend-001",
          "expiresAt": "2026-12-31T00:00:00Z"
        }
        """.trimIndent()

    // 토큰 검증 응답 바디를 반환한다.
    private fun validateResponseBody(): String =
        """
        {
          "valid": true,
          "username": "operator-1",
          "expiresAt": "2026-12-31T00:00:00Z"
        }
        """.trimIndent()

    // 회원가입 응답 바디를 반환한다.
    private fun registerResponseBody(): String =
        """
        {
          "username": "operator-1",
          "createdAt": "2026-12-31T00:00:00Z"
        }
        """.trimIndent()

    // 이벤트 수집 응답 바디를 반환한다.
    private fun ingestResponseBody(): String =
        """
        {
          "eventId": "event-001",
          "status": "ACCEPTED",
          "observedAt": "2026-12-31T00:00:00Z"
        }
        """.trimIndent()

    // 요약 응답 바디를 반환한다.
    private fun summaryResponseBody(totalEvents: Long): String =
        """
        {
          "generatedAt": "2026-02-09T00:00:00Z",
          "totalEvents": $totalEvents,
          "regions": [
            {
              "region": "SEOUL",
              "totalEvents": $totalEvents,
              "averageSpeedKph": 42.8,
              "latestCongestionLevel": 3
            }
          ]
        }
        """.trimIndent()

    // 최근 이벤트 응답 바디를 반환한다.
    private fun recentResponseBody(): String =
        """
        [
          {
            "eventId": "event-001",
            "traceId": "trace-001",
            "region": "SEOUL",
            "roadName": "Gangnam-daero",
            "averageSpeedKph": 38,
            "congestionLevel": 4,
            "observedAt": "2026-02-09T00:00:00Z"
          }
        ]
        """.trimIndent()

    // GET 요청을 전송한다.
    private fun get(path: String, headers: Map<String, String> = emptyMap()): HttpResponse<String> {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .GET()

        headers.forEach { (name, value) -> builder.header(name, value) }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    // JSON POST 요청을 전송한다.
    private fun postJson(path: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
