package com.devbattery.infinitytraffic.frontend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class TrafficFrontendApplication

fun main(args: Array<String>) {
    runApplication<TrafficFrontendApplication>(*args)
}
