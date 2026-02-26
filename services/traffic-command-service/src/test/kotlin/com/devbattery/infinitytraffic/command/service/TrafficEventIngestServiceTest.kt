package com.devbattery.infinitytraffic.command.service

import com.devbattery.infinitytraffic.command.web.TrafficEventIngestRequest
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * TrafficEventIngestService 단위 테스트다.
 */
@ExtendWith(MockitoExtension::class)
class TrafficEventIngestServiceTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<Any, Any>

    // Kafka 발행 성공 시 수집 메트릭(건수/지연)이 기록되는지 검증한다.
    @Test
    fun ingestShouldRecordAcceptedMetricsWhenKafkaSendSucceeds() {
        val meterRegistry = SimpleMeterRegistry()
        val service =
            TrafficEventIngestService(
                trafficCommandProperties = TrafficCommandProperties(topic = "traffic.events.v1"),
                kafkaTemplate = kafkaTemplate,
                meterRegistry = meterRegistry,
                clock = Clock.fixed(Instant.parse("2026-02-26T00:00:00Z"), ZoneOffset.UTC),
            )

        @Suppress("UNCHECKED_CAST")
        val sendResult = org.mockito.Mockito.mock(SendResult::class.java) as SendResult<Any, Any>
        given(kafkaTemplate.send(anyString(), any(), any()))
            .willReturn(CompletableFuture.completedFuture(sendResult))

        val response =
            service.ingest(
                traceId = "trace-001",
                request =
                    TrafficEventIngestRequest(
                        region = "seoul",
                        roadName = "강변북로",
                        averageSpeedKph = 42,
                        congestionLevel = 3,
                        observedAt = null,
                    ),
            )

        assertThat(response.status).isEqualTo("ACCEPTED")
        assertThat(
            meterRegistry
                .get(TrafficEventIngestService.INGEST_COUNTER_NAME)
                .tags("status", "accepted", "region", "SEOUL", "exception", "none")
                .counter()
                .count(),
        ).isEqualTo(1.0)
        assertThat(
            meterRegistry
                .get(TrafficEventIngestService.INGEST_LATENCY_NAME)
                .tags("status", "accepted", "region", "SEOUL")
                .timer()
                .count(),
        ).isEqualTo(1)
    }

    // Kafka 발행 실패 시 실패 메트릭이 기록되고 예외가 전파되는지 검증한다.
    @Test
    fun ingestShouldRecordFailedMetricsWhenKafkaSendFails() {
        val meterRegistry = SimpleMeterRegistry()
        val service =
            TrafficEventIngestService(
                trafficCommandProperties = TrafficCommandProperties(topic = "traffic.events.v1"),
                kafkaTemplate = kafkaTemplate,
                meterRegistry = meterRegistry,
                clock = Clock.fixed(Instant.parse("2026-02-26T00:00:00Z"), ZoneOffset.UTC),
            )

        val failedFuture = CompletableFuture<SendResult<Any, Any>>().apply {
            completeExceptionally(IllegalStateException("Kafka broker unavailable"))
        }
        given(kafkaTemplate.send(anyString(), any(), any())).willReturn(failedFuture)

        assertThatThrownBy {
            service.ingest(
                traceId = "trace-002",
                request =
                    TrafficEventIngestRequest(
                        region = "BUSAN",
                        roadName = "해운대로",
                        averageSpeedKph = 30,
                        congestionLevel = 4,
                        observedAt = Instant.parse("2026-02-26T00:00:00Z"),
                    ),
            )
        }.isInstanceOf(ExecutionException::class.java)

        assertThat(
            meterRegistry
                .get(TrafficEventIngestService.INGEST_COUNTER_NAME)
                .tags("status", "failed", "region", "BUSAN", "exception", "ExecutionException")
                .counter()
                .count(),
        ).isEqualTo(1.0)
        assertThat(
            meterRegistry
                .get(TrafficEventIngestService.INGEST_LATENCY_NAME)
                .tags("status", "failed", "region", "BUSAN")
                .timer()
                .count(),
        ).isEqualTo(1)
    }
}
