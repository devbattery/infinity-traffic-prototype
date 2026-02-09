package com.devbattery.infinitytraffic.query.service

import com.devbattery.infinitytraffic.query.domain.entity.TrafficEventEntity
import com.devbattery.infinitytraffic.query.domain.entity.TrafficRegionProjectionEntity
import com.devbattery.infinitytraffic.query.repository.TrafficEventRepository
import com.devbattery.infinitytraffic.query.repository.TrafficRegionProjectionRepository
import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficRegionSummary
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * Kafka 이벤트를 영속화하고 조회 모델로 투영하는 서비스다.
 */
@Service
class TrafficProjectionService(
    private val trafficEventRepository: TrafficEventRepository,
    private val trafficRegionProjectionRepository: TrafficRegionProjectionRepository,
    private val clock: Clock,
) {

    // Kafka에서 수신한 이벤트를 원본/집계 테이블에 반영한다.
    @Transactional
    fun applyEvent(event: TrafficEventMessage) {
        // 동일 eventId는 중복 처리하지 않아 at-least-once 소비를 안전하게 만든다.
        if (trafficEventRepository.existsById(event.eventId)) {
            return
        }

        val now = Instant.now(clock)

        val eventEntity = TrafficEventEntity(
            eventId = event.eventId,
            traceId = event.traceId,
            region = event.region,
            roadName = event.roadName,
            averageSpeedKph = event.averageSpeedKph,
            congestionLevel = event.congestionLevel,
            observedAt = event.observedAt,
            createdAt = now,
        )
        trafficEventRepository.save(eventEntity)

        val projection = trafficRegionProjectionRepository.findById(event.region)
            .orElse(
                TrafficRegionProjectionEntity(
                    region = event.region,
                    totalEvents = 0,
                    speedSum = 0,
                    latestCongestionLevel = 0,
                    updatedAt = now,
                ),
            )

        projection.totalEvents += 1
        projection.speedSum += event.averageSpeedKph.toLong()
        projection.latestCongestionLevel = event.congestionLevel
        projection.updatedAt = now

        trafficRegionProjectionRepository.save(projection)
    }

    // 지역 필터 유무에 따라 교통 요약 정보를 반환한다.
    @Transactional(readOnly = true)
    fun summary(region: String?): TrafficSummaryResponse {
        val projections = if (region.isNullOrBlank()) {
            trafficRegionProjectionRepository.findAll().sortedBy { it.region }
        } else {
            trafficRegionProjectionRepository.findById(region).map { listOf(it) }.orElse(emptyList())
        }

        val regionSummaries = projections.map {
            TrafficRegionSummary(
                region = it.region,
                totalEvents = it.totalEvents,
                averageSpeedKph = if (it.totalEvents == 0L) 0.0 else it.speedSum.toDouble() / it.totalEvents,
                latestCongestionLevel = it.latestCongestionLevel,
            )
        }

        return TrafficSummaryResponse(
            generatedAt = Instant.now(clock),
            totalEvents = trafficEventRepository.count(),
            regions = regionSummaries,
        )
    }

    // 최신순 이벤트 목록을 제한 개수만큼 반환한다.
    @Transactional(readOnly = true)
    fun recent(limit: Int): List<TrafficEventMessage> {
        return trafficEventRepository
            .findAllByOrderByObservedAtDescEventIdDesc(PageRequest.of(0, limit))
            .map {
                TrafficEventMessage(
                    eventId = it.eventId,
                    traceId = it.traceId,
                    region = it.region,
                    roadName = it.roadName,
                    averageSpeedKph = it.averageSpeedKph,
                    congestionLevel = it.congestionLevel,
                    observedAt = it.observedAt,
                )
            }
    }
}
