package com.devbattery.infinitytraffic.auth.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * 회원가입 요청 모델이다.
 */
data class RegisterRequest(
    @field:NotBlank(message = "username은 필수입니다.")
    val username: String,
    @field:Size(min = 8, message = "password는 최소 8자 이상이어야 합니다.")
    val password: String,
)

/**
 * 로그인 요청 모델이다.
 */
data class LoginRequest(
    @field:NotBlank(message = "username은 필수입니다.")
    val username: String,
    @field:NotBlank(message = "password는 필수입니다.")
    val password: String,
)

/**
 * 회원가입 응답 모델이다.
 */
data class RegisterResponse(
    val username: String,
    val createdAt: Instant,
)

/**
 * 토큰 발급 응답 모델이다.
 */
data class AuthTokenResponse(
    val tokenType: String,
    val accessToken: String,
    val expiresAt: Instant,
)

/**
 * 토큰 검증 응답 모델이다.
 */
data class TokenValidationResponse(
    val valid: Boolean,
    val username: String?,
    val expiresAt: Instant?,
)
