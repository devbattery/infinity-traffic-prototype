package com.devbattery.infinitytraffic.frontend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * 프론트엔드 내부 HTTP 클라이언트 빈을 구성한다.
 */
@Configuration
class FrontendHttpClientConfig {

    // 게이트웨이 호출에 사용할 RestClient.Builder를 등록한다.
    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()

    // 프론트엔드 JSON 직렬화/역직렬화에 사용할 ObjectMapper를 등록한다.
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
}
