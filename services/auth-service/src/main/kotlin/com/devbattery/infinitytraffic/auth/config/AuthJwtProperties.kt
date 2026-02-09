package com.devbattery.infinitytraffic.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 생성/검증 시 필요한 보안 설정값을 관리한다.
 */
@ConfigurationProperties(prefix = "auth.jwt")
data class AuthJwtProperties(
    var issuer: String = "infinity-traffic-auth",
    var secret: String = "infinity-traffic-default-secret-key-please-change-this-now-2026",
    var accessTokenExpireSeconds: Long = 3600,
)
