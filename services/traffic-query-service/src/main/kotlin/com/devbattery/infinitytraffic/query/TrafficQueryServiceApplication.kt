package com.devbattery.infinitytraffic.query

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TrafficQueryServiceApplication

// 교통 쿼리 서비스 애플리케이션을 시작한다.
fun main(args: Array<String>) {
    runApplication<TrafficQueryServiceApplication>(*args)
}
