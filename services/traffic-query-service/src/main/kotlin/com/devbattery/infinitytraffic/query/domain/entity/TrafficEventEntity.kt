package com.devbattery.infinitytraffic.query.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 조회 모델 재구성을 위해 저장하는 교통 원본 이벤트 엔티티다.
 */
@Entity
@Table(name = "traffic_event")
class TrafficEventEntity(
    @Id
    @Column(name = "event_id", nullable = false, length = 64)
    val eventId: String,

    @Column(name = "trace_id", nullable = false, length = 64)
    var traceId: String,

    @Column(name = "region", nullable = false, length = 100)
    var region: String,

    @Column(name = "road_name", nullable = false, length = 200)
    var roadName: String,

    @Column(name = "average_speed_kph", nullable = false)
    var averageSpeedKph: Int,

    @Column(name = "congestion_level", nullable = false)
    var congestionLevel: Int,

    @Column(name = "observed_at", nullable = false)
    var observedAt: Instant,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
