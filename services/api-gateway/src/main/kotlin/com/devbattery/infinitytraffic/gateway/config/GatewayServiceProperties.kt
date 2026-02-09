package com.devbattery.infinitytraffic.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 게이트웨이가 호출할 다운스트림 서비스의 기본 주소를 관리한다.
 */
@ConfigurationProperties(prefix = "gateway.services")
data class GatewayServiceProperties(
    var auth: String = "http://localhost:8081",
    var command: String = "http://localhost:8082",
    var query: String = "http://localhost:8083",
)
