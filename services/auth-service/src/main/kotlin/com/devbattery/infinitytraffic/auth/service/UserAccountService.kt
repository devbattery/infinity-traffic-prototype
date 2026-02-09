package com.devbattery.infinitytraffic.auth.service

import com.devbattery.infinitytraffic.auth.domain.UserAccount
import com.devbattery.infinitytraffic.auth.domain.entity.UserAccountEntity
import com.devbattery.infinitytraffic.auth.repository.UserAccountRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 계정 등록과 비밀번호 검증을 담당하는 도메인 서비스다.
 */
@Service
class UserAccountService(
    private val userAccountRepository: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    // 새로운 사용자를 등록한다.
    @Transactional
    fun register(username: String, rawPassword: String): UserAccount {
        val normalizedUsername = username.trim()
        require(normalizedUsername.isNotBlank()) { "username은 공백일 수 없습니다." }
        require(!userAccountRepository.existsByUsername(normalizedUsername)) { "이미 존재하는 username입니다." }

        val entity = userAccountRepository.save(
            UserAccountEntity(
                username = normalizedUsername,
                passwordHash = passwordEncoder.encode(rawPassword)
                    ?: throw IllegalStateException("비밀번호 해시 생성에 실패했습니다."),
                createdAt = Instant.now(),
            ),
        )

        return entity.toDomain()
    }

    // 사용자 자격 증명을 검증한다.
    @Transactional(readOnly = true)
    fun authenticate(username: String, rawPassword: String): UserAccount {
        val account = userAccountRepository.findByUsername(username.trim())
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")
        require(passwordEncoder.matches(rawPassword, account.passwordHash)) { "비밀번호가 올바르지 않습니다." }
        return account.toDomain()
    }

    // 엔티티를 도메인 모델로 변환한다.
    private fun UserAccountEntity.toDomain(): UserAccount {
        return UserAccount(
            username = username,
            passwordHash = passwordHash,
            createdAt = createdAt,
        )
    }
}
