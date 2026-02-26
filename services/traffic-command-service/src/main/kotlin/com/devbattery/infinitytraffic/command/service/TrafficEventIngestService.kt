package com.devbattery.infinitytraffic.command.service

import com.devbattery.infinitytraffic.command.web.TrafficEventIngestRequest
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficIngestResult
import io.micrometer.core.instrument.MeterRegistry
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
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {

    // 이벤트를 생성하고 Kafka 토픽으로 발행한 뒤 결과를 반환한다.
    fun ingest(traceId: String, request: TrafficEventIngestRequest): TrafficIngestResult {
        val eventId = UUID.randomUUID().toString()
        val observedAt = request.observedAt ?: Instant.now(clock)
        val normalizedRegion = request.region.trim().uppercase()
        val startedAtNanos = System.nanoTime()

        val message = TrafficEventMessage(
            eventId = eventId,
            traceId = traceId,
            region = normalizedRegion,
            roadName = request.roadName.trim(),
            averageSpeedKph = request.averageSpeedKph,
            congestionLevel = request.congestionLevel,
            observedAt = observedAt,
        )

        try {
            // 발행 성공 여부를 빠르게 확인해 API 응답 신뢰도를 높인다.
            kafkaTemplate.send(trafficCommandProperties.topic, message.eventId, message).get(3, TimeUnit.SECONDS)
            recordMetrics(
                status = "accepted",
                region = message.region,
                startedAtNanos = startedAtNanos,
            )
        } catch (error: Exception) {
            recordMetrics(
                status = "failed",
                region = message.region,
                startedAtNanos = startedAtNanos,
                exceptionName = error.javaClass.simpleName ?: "UnknownException",
            )
            throw error
        }

        return TrafficIngestResult(
            eventId = eventId,
            status = "ACCEPTED",
            observedAt = observedAt,
        )
    }

    // 성공/실패 건수와 처리 지연을 함께 기록해 운영자가 이상 징후를 빠르게 식별할 수 있게 한다.
    private fun recordMetrics(status: String, region: String, startedAtNanos: Long, exceptionName: String = "none") {
        meterRegistry.counter(
            INGEST_COUNTER_NAME,
            "status",
            status,
            "region",
            region,
            "exception",
            exceptionName,
        ).increment()

        meterRegistry.timer(
            INGEST_LATENCY_NAME,
            "status",
            status,
            "region",
            region,
        ).record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS)
    }

    companion object {
        const val INGEST_COUNTER_NAME: String = "traffic_command_ingest_total"
        const val INGEST_LATENCY_NAME: String = "traffic_command_ingest_latency"
    }
}
