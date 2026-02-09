package com.devbattery.infinitytraffic.query.service

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 쿼리 서비스의 Kafka 소비 설정을 보관한다.
 */
@ConfigurationProperties(prefix = "traffic.kafka")
data class TrafficQueryProperties(
    var topic: String = "traffic.events.v1",
    var groupId: String = "traffic-query-service",
)
