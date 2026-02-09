package com.devbattery.infinitytraffic.frontend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 프론트엔드가 호출할 API 게이트웨이 주소를 보관한다.
 */
@ConfigurationProperties(prefix = "frontend.gateway")
data class FrontendGatewayProperties(
    val baseUrl: String = "http://localhost:8080",
)
