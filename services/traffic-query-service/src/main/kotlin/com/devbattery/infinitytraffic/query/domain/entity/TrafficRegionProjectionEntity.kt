package com.devbattery.infinitytraffic.query.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 지역별 교통 집계 조회 모델을 저장하는 엔티티다.
 */
@Entity
@Table(name = "traffic_region_projection")
class TrafficRegionProjectionEntity(
    @Id
    @Column(name = "region", nullable = false, length = 100)
    val region: String,

    @Column(name = "total_events", nullable = false)
    var totalEvents: Long,

    @Column(name = "speed_sum", nullable = false)
    var speedSum: Long,

    @Column(name = "latest_congestion_level", nullable = false)
    var latestCongestionLevel: Int,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
