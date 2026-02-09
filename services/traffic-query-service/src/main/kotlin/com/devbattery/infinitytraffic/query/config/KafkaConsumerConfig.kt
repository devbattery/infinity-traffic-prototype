package com.devbattery.infinitytraffic.query.config

import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

/**
 * 쿼리 서비스의 Kafka Consumer 빈을 명시적으로 구성한다.
 */
@Configuration
@EnableKafka
class KafkaConsumerConfig(
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private val bootstrapServers: String,
    @Value("\${traffic.kafka.group-id:traffic-query-service}")
    private val groupId: String,
) {

    // 교통 이벤트 소비용 ConsumerFactory를 생성한다.
    @Bean
    fun consumerFactory(): ConsumerFactory<String, TrafficEventMessage> {
        val jsonDeserializer = JsonDeserializer(TrafficEventMessage::class.java)
        jsonDeserializer.addTrustedPackages("*")
        jsonDeserializer.setUseTypeMapperForKey(false)

        val configs = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        )

        return DefaultKafkaConsumerFactory(configs, StringDeserializer(), jsonDeserializer)
    }

    // @KafkaListener가 사용할 기본 리스너 컨테이너 팩토리를 생성한다.
    @Bean(name = ["kafkaListenerContainerFactory"])
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, TrafficEventMessage>,
    ): ConcurrentKafkaListenerContainerFactory<String, TrafficEventMessage> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, TrafficEventMessage>()
        factory.setConsumerFactory(consumerFactory)
        return factory
    }
}
