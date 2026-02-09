package com.devbattery.infinitytraffic.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock

/**
 * 인증 서비스에서 공통으로 사용하는 빈을 구성한다.
 */
@Configuration
class AuthCommonConfig {

    // 테스트와 운영에서 동일하게 사용할 시스템 시계를 주입한다.
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    // 비밀번호 해시/검증에 사용할 PasswordEncoder를 제공한다.
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
