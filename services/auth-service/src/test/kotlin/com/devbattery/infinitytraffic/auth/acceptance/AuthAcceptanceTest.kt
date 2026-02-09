package com.devbattery.infinitytraffic.auth.acceptance

import com.devbattery.infinitytraffic.auth.web.AuthTokenResponse
import com.devbattery.infinitytraffic.auth.web.LoginRequest
import com.devbattery.infinitytraffic.auth.web.RegisterRequest
import com.devbattery.infinitytraffic.auth.web.RegisterResponse
import com.devbattery.infinitytraffic.auth.web.TokenValidationResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/**
 * 인증 서비스 인수 테스트를 수행한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthAcceptanceTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    // 회원가입부터 로그인, 토큰 검증까지 실제 사용자 인증 플로우를 검증한다.
    @Test
    fun registerLoginValidateFlow() {
        val username = "loaduser-${UUID.randomUUID()}"
        val password = "Password!123"

        val registerResponse = postJson(
            path = "/api/v1/auth/register",
            body = RegisterRequest(username = username, password = password),
        )

        assertThat(registerResponse.statusCode()).isEqualTo(200)
        val registerBody = objectMapper.readValue(registerResponse.body(), RegisterResponse::class.java)
        assertThat(registerBody.username).isEqualTo(username)

        val loginResponse = postJson(
            path = "/api/v1/auth/login",
            body = LoginRequest(username = username, password = password),
        )

        assertThat(loginResponse.statusCode()).isEqualTo(200)
        val loginBody = objectMapper.readValue(loginResponse.body(), AuthTokenResponse::class.java)
        assertThat(loginBody.accessToken).isNotBlank()

        val validateRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/api/v1/auth/validate"))
            .header("Authorization", "Bearer ${loginBody.accessToken}")
            .GET()
            .build()

        val validateResponse = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString())
        assertThat(validateResponse.statusCode()).isEqualTo(200)

        val validateBody = objectMapper.readValue(validateResponse.body(), TokenValidationResponse::class.java)
        assertThat(validateBody.valid).isTrue()
        assertThat(validateBody.username).isEqualTo(username)
    }

    // JSON POST 요청을 전송하고 문자열 응답을 반환한다.
    private fun postJson(path: String, body: Any): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
