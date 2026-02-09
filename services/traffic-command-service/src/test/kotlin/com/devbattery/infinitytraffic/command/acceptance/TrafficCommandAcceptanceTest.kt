package com.devbattery.infinitytraffic.command.acceptance

import com.devbattery.infinitytraffic.command.web.TrafficEventIngestRequest
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficIngestResult
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 커맨드 서비스 인수 테스트를 수행한다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"],
)
@EmbeddedKafka(partitions = 1, topics = ["traffic.events.v1"])
class TrafficCommandAcceptanceTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private var consumer: Consumer<String, TrafficEventMessage>? = null

    // 테스트 종료 후 Kafka consumer 리소스를 정리한다.
    @AfterEach
    fun tearDown() {
        consumer?.close()
    }

    // 교통 이벤트 수집 요청이 Kafka 토픽으로 발행되는지 검증한다.
    @Test
    fun ingestEventShouldPublishKafkaMessage() {
        val traceId = "trace-${UUID.randomUUID()}"

        val request = TrafficEventIngestRequest(
            region = "SEOUL",
            roadName = "강변북로",
            averageSpeedKph = 42,
            congestionLevel = 4,
            observedAt = Instant.now(),
        )

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/api/v1/traffic/events"))
            .header("Content-Type", "application/json")
            .header(TraceHeaders.TRACE_ID, traceId)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        assertThat(response.statusCode()).isEqualTo(202)
        val responseBody = objectMapper.readValue(response.body(), TrafficIngestResult::class.java)
        assertThat(responseBody.status).isEqualTo("ACCEPTED")

        val kafkaConsumer = createConsumer()
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(kafkaConsumer, "traffic.events.v1")

        val records = kafkaConsumer.poll(Duration.ofSeconds(10))
        assertThat(records.isEmpty).isFalse()

        val message = records.iterator().next().value()
        assertThat(message.region).isEqualTo("SEOUL")
        assertThat(message.roadName).isEqualTo("강변북로")
        assertThat(message.traceId).isEqualTo(traceId)
        assertThat(message.averageSpeedKph).isEqualTo(42)
        assertThat(message.congestionLevel).isEqualTo(4)
    }

    // 테스트 전용 Kafka consumer를 생성한다.
    private fun createConsumer(): Consumer<String, TrafficEventMessage> {
        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to embeddedKafkaBroker.brokersAsString,
            ConsumerConfig.GROUP_ID_CONFIG to "traffic-command-acceptance",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        )

        val jsonDeserializer = JsonDeserializer(TrafficEventMessage::class.java)
        jsonDeserializer.addTrustedPackages("*")

        val kafkaConsumer = DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            jsonDeserializer,
        ).createConsumer()

        consumer = kafkaConsumer
        return kafkaConsumer
    }
}
