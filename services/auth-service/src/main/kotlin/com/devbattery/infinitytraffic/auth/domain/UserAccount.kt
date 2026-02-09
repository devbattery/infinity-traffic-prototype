package com.devbattery.infinitytraffic.auth.domain

import java.time.Instant

/**
 * 인증 도메인에서 관리하는 사용자 계정 정보다.
 */
data class UserAccount(
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
)
