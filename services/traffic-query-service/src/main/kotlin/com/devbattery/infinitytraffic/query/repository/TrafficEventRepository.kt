package com.devbattery.infinitytraffic.query.repository

import com.devbattery.infinitytraffic.query.domain.entity.TrafficEventEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교통 이벤트 원본 저장소다.
 */
interface TrafficEventRepository : JpaRepository<TrafficEventEntity, String> {

    // 최근 이벤트를 관측 시각 역순으로 조회한다.
    fun findAllByOrderByObservedAtDescEventIdDesc(pageable: Pageable): List<TrafficEventEntity>
}
