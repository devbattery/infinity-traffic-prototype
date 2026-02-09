package com.devbattery.infinitytraffic.query.acceptance

import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID

/**
 * 쿼리 서비스 인수 테스트를 수행한다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"],
)
@EmbeddedKafka(partitions = 1, topics = ["traffic.events.v1"])
class TrafficQueryAcceptanceTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    // Kafka 이벤트 수신 후 조회 API가 투영 결과를 반환하는지 검증한다.
    @Test
    fun summaryAndRecentApisShouldReflectConsumedEvent() {
        val message = TrafficEventMessage(
            eventId = UUID.randomUUID().toString(),
            traceId = "trace-${UUID.randomUUID()}",
            region = "BUSAN",
            roadName = "MarineCity",
            averageSpeedKph = 55,
            congestionLevel = 2,
            observedAt = Instant.now(),
        )

        val kafkaTemplate = createKafkaTemplate()
        kafkaTemplate.send("traffic.events.v1", message.eventId, message).get()
        kafkaTemplate.flush()

        // 비동기 소비를 고려해 짧은 시간 동안 재시도하며 결과를 확인한다.
        val summary = waitForSummary(region = "BUSAN")
        assertThat(summary.totalEvents).isGreaterThanOrEqualTo(1)
        assertThat(summary.regions).hasSize(1)
        assertThat(summary.regions.first().region).isEqualTo("BUSAN")
        assertThat(summary.regions.first().latestCongestionLevel).isEqualTo(2)

        val recentResponse = get("/api/v1/traffic/events/recent?limit=10")
        assertThat(recentResponse.statusCode()).isEqualTo(200)

        val recentBody = objectMapper.readValue(
            recentResponse.body(),
            object : TypeReference<List<TrafficEventMessage>>() {},
        )

        assertThat(recentBody.map { it.eventId }).contains(message.eventId)
    }

    // 조회 결과에 이벤트가 반영될 때까지 재시도한다.
    private fun waitForSummary(region: String): TrafficSummaryResponse {
        var last: TrafficSummaryResponse? = null

        repeat(30) {
            val response = get("/api/v1/traffic/summary?region=$region")
            if (response.statusCode() == 200) {
                val body = objectMapper.readValue(response.body(), TrafficSummaryResponse::class.java)
                if (body.regions.isNotEmpty()) {
                    return body
                }
                last = body
            }

            Thread.sleep(200)
        }

        throw IllegalStateException("요약 투영 결과가 시간 내에 생성되지 않았습니다. last=$last")
    }

    // GET 요청을 전송한다.
    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    // 테스트용 KafkaTemplate을 생성한다.
    private fun createKafkaTemplate(): KafkaTemplate<String, TrafficEventMessage> {
        val configs = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to embeddedKafkaBroker.brokersAsString,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        )

        val producerFactory = DefaultKafkaProducerFactory<String, TrafficEventMessage>(
            configs,
            StringSerializer(),
            JsonSerializer<TrafficEventMessage>(),
        )

        return KafkaTemplate(producerFactory)
    }
}
