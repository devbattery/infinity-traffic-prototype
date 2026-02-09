package com.devbattery.infinitytraffic.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AuthServiceApplication

// 인증 서비스 애플리케이션을 시작한다.
fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
