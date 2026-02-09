package com.devbattery.infinitytraffic.command.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 커맨드 서비스 공통 빈을 설정한다.
 */
@Configuration
class CommandCommonConfig {

    // 이벤트 시각 계산에 사용하는 시스템 시계를 제공한다.
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
