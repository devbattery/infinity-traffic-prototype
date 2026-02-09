package com.devbattery.infinitytraffic.auth.service

import com.devbattery.infinitytraffic.auth.security.JwtTokenService
import com.devbattery.infinitytraffic.auth.web.AuthTokenResponse
import com.devbattery.infinitytraffic.auth.web.RegisterResponse
import com.devbattery.infinitytraffic.auth.web.TokenValidationResponse
import org.springframework.stereotype.Service

/**
 * 인증 유스케이스를 조합해 컨트롤러에 제공하는 파사드 서비스다.
 */
@Service
class AuthFacadeService(
    private val userAccountService: UserAccountService,
    private val jwtTokenService: JwtTokenService,
) {

    // 회원가입을 처리한다.
    fun register(username: String, password: String): RegisterResponse {
        val user = userAccountService.register(username, password)
        return RegisterResponse(
            username = user.username,
            createdAt = user.createdAt,
        )
    }

    // 로그인 후 JWT를 발급한다.
    fun login(username: String, password: String): AuthTokenResponse {
        val user = userAccountService.authenticate(username, password)
        val issueResult = jwtTokenService.issueAccessToken(user.username)

        return AuthTokenResponse(
            tokenType = "Bearer",
            accessToken = issueResult.accessToken,
            expiresAt = issueResult.expiresAt,
        )
    }

    // 토큰을 검증해서 사용자 식별 정보와 만료 시각을 반환한다.
    fun validate(accessToken: String): TokenValidationResponse {
        val claims = jwtTokenService.parseClaims(accessToken)

        return TokenValidationResponse(
            valid = true,
            username = claims.subject,
            expiresAt = claims.expiration.toInstant(),
        )
    }
}
