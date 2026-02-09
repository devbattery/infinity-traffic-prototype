package com.devbattery.infinitytraffic.command

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TrafficCommandServiceApplication

// 교통 커맨드 서비스 애플리케이션을 시작한다.
fun main(args: Array<String>) {
    runApplication<TrafficCommandServiceApplication>(*args)
}
