package com.devbattery.infinitytraffic.auth.web

import com.devbattery.infinitytraffic.auth.service.AuthFacadeService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 관련 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authFacadeService: AuthFacadeService,
) {

    // 회원가입을 처리한다.
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): RegisterResponse {
        return authFacadeService.register(
            username = request.username,
            password = request.password,
        )
    }

    // 로그인 후 JWT 액세스 토큰을 발급한다.
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): AuthTokenResponse {
        return authFacadeService.login(
            username = request.username,
            password = request.password,
        )
    }

    // Authorization 헤더의 Bearer 토큰을 검증한다.
    @GetMapping("/validate")
    fun validate(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
    ): TokenValidationResponse {
        require(authorization.startsWith("Bearer ")) { "Bearer 토큰 형식이 아닙니다." }
        val accessToken = authorization.removePrefix("Bearer ").trim()
        return authFacadeService.validate(accessToken)
    }
}
