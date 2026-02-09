package com.devbattery.infinitytraffic.auth.repository

import com.devbattery.infinitytraffic.auth.domain.entity.UserAccountEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 사용자 계정 영속성 저장소다.
 */
interface UserAccountRepository : JpaRepository<UserAccountEntity, Long> {

    // username으로 계정을 조회한다.
    fun findByUsername(username: String): UserAccountEntity?

    // username 중복 여부를 확인한다.
    fun existsByUsername(username: String): Boolean
}
