package com.devbattery.infinitytraffic.query.service

import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka 교통 이벤트를 소비해 조회 모델을 갱신한다.
 */
@Component
class TrafficEventConsumer(
    private val trafficProjectionService: TrafficProjectionService,
) {

    // 수신된 교통 이벤트를 읽기 모델에 반영한다.
    @KafkaListener(
        topics = ["\${traffic.kafka.topic:traffic.events.v1}"],
        groupId = "\${traffic.kafka.group-id:traffic-query-service}",
    )
    fun consume(message: TrafficEventMessage) {
        trafficProjectionService.applyEvent(message)
    }
}
