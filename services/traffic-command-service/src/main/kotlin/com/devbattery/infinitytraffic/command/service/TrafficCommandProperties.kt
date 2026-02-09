package com.devbattery.infinitytraffic.command.service

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 커맨드 서비스의 Kafka 발행 설정을 보관한다.
 */
@ConfigurationProperties(prefix = "traffic.kafka")
data class TrafficCommandProperties(
    var topic: String = "traffic.events.v1",
)
