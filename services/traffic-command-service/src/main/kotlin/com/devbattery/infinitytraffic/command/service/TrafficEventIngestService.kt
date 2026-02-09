package com.devbattery.infinitytraffic.command.service

import com.devbattery.infinitytraffic.command.web.TrafficEventIngestRequest
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficIngestResult
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 수집된 교통 이벤트를 표준 계약으로 변환해 Kafka로 발행한다.
 */
@Service
class TrafficEventIngestService(
    private val trafficCommandProperties: TrafficCommandProperties,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val clock: Clock,
) {

    // 이벤트를 생성하고 Kafka 토픽으로 발행한 뒤 결과를 반환한다.
    fun ingest(traceId: String, request: TrafficEventIngestRequest): TrafficIngestResult {
        val eventId = UUID.randomUUID().toString()
        val observedAt = request.observedAt ?: Instant.now(clock)

        val message = TrafficEventMessage(
            eventId = eventId,
            traceId = traceId,
            region = request.region.trim(),
            roadName = request.roadName.trim(),
            averageSpeedKph = request.averageSpeedKph,
            congestionLevel = request.congestionLevel,
            observedAt = observedAt,
        )

        // 발행 성공 여부를 빠르게 확인해 API 응답 신뢰도를 높인다.
        kafkaTemplate.send(trafficCommandProperties.topic, message.eventId, message).get(3, TimeUnit.SECONDS)

        return TrafficIngestResult(
            eventId = eventId,
            status = "ACCEPTED",
            observedAt = observedAt,
        )
    }
}
