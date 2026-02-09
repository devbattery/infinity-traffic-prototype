package com.devbattery.infinitytraffic.frontend.acceptance

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * 타임리프 프론트엔드 인수 테스트를 수행한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrafficFrontendAcceptanceTest {

    @LocalServerPort
    private var port: Int = 0

    private val noRedirectClient: HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
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

    // 대시보드 페이지가 게이트웨이 데이터로 렌더링되는지 검증한다.
    @Test
    fun dashboardShouldRenderWithGatewayData() {
        installDispatcher(totalEvents = 12)

        val response = get(path = "/dashboard")

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("Infinity Traffic Control Center")
        assertThat(response.body()).contains("SEOUL")

        val requestPaths = collectRequestPaths(2)
        assertThat(requestPaths).contains("/api/traffic/summary")
        assertThat(requestPaths.any { it.startsWith("/api/traffic/events/recent") }).isTrue()
    }

    // 로그인 후 세션이 유지된 상태에서 Ajax 스냅샷이 인증 사용자 정보를 반환하는지 검증한다.
    @Test
    fun loginThenSnapshotShouldReturnAuthenticatedUser() {
        installDispatcher(totalEvents = 25)

        val loginResponse =
            postForm(
                path = "/auth/login",
                formData = mapOf("username" to "operator-1", "password" to "Password!1234"),
            )

        assertThat(loginResponse.statusCode()).isEqualTo(302)
        assertThat(loginResponse.headers().firstValue("location").orElse("")).contains("/dashboard")

        val sessionCookie = loginResponse.headers().firstValue("set-cookie").orElse("").substringBefore(';')
        assertThat(sessionCookie).startsWith("JSESSIONID=")

        val snapshotResponse = get(path = "/ui/api/dashboard", headers = mapOf("Cookie" to sessionCookie))
        assertThat(snapshotResponse.statusCode()).isEqualTo(200)
        val snapshotJson = objectMapper.readTree(snapshotResponse.body())
        assertThat(snapshotJson["authenticated"].asBoolean()).isTrue()
        assertThat(snapshotJson["username"].asText()).isEqualTo("operator-1")

        val requestPaths = collectRequestPaths(6)
        assertThat(requestPaths).contains("/api/auth/login")
        assertThat(requestPaths).contains("/api/auth/validate")
        assertThat(requestPaths).contains("/api/traffic/summary")
        assertThat(requestPaths.any { it.startsWith("/api/traffic/events/recent") }).isTrue()
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
                        path == "/api/traffic/summary" -> jsonResponse(summaryResponseBody(totalEvents))
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

        return noRedirectClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    // application/x-www-form-urlencoded POST 요청을 전송한다.
    private fun postForm(path: String, formData: Map<String, String>): HttpResponse<String> {
        val body = formData.entries.joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return noRedirectClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    // URL 인코딩을 수행한다.
    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
