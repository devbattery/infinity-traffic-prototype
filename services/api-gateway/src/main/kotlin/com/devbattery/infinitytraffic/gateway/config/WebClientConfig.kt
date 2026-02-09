package com.devbattery.infinitytraffic.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 게이트웨이의 다운스트림 호출에 사용할 WebClient 빈을 구성한다.
 */
@Configuration
class WebClientConfig {

    // 서비스 프록시 계층에서 재사용할 WebClient.Builder를 등록한다.
    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()
}
