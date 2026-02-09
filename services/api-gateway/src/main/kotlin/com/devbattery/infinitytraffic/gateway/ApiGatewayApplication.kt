package com.devbattery.infinitytraffic.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ApiGatewayApplication

// API 게이트웨이 애플리케이션을 시작한다.
fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
