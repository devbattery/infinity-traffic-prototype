package com.devbattery.infinitytraffic.auth.security

import com.devbattery.infinitytraffic.auth.config.AuthJwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

/**
 * JWT 생성과 검증을 담당하는 보안 컴포넌트다.
 */
@Component
class JwtTokenService(
    private val authJwtProperties: AuthJwtProperties,
) {

    // 액세스 토큰을 발급하고 만료 시각을 함께 반환한다.
    fun issueAccessToken(username: String): TokenIssueResult {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(authJwtProperties.accessTokenExpireSeconds)

        val token = Jwts.builder()
            .issuer(authJwtProperties.issuer)
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()

        return TokenIssueResult(
            accessToken = token,
            expiresAt = expiresAt,
        )
    }

    // 액세스 토큰을 검증하고 클레임을 반환한다.
    fun parseClaims(accessToken: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload
    }

    // HS256 서명 키를 생성한다.
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(authJwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
    }
}

/**
 * 토큰 발급 결과를 표현한다.
 */
data class TokenIssueResult(
    val accessToken: String,
    val expiresAt: Instant,
)
