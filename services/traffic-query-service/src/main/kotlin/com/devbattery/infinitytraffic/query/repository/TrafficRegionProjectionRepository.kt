package com.devbattery.infinitytraffic.query.repository

import com.devbattery.infinitytraffic.query.domain.entity.TrafficRegionProjectionEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 지역별 교통 집계 조회 모델 저장소다.
 */
interface TrafficRegionProjectionRepository : JpaRepository<TrafficRegionProjectionEntity, String>
