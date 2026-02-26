package com.devbattery.infinitytraffic.frontend.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * PlatformGuideService 단위 테스트다.
 */
class PlatformGuideServiceTest {

    // 안내 응답이 서비스 목적과 운영 흐름을 충분히 포함하는지 검증한다.
    @Test
    fun overviewShouldContainPurposeAndFlow() {
        val service = PlatformGuideService()

        val overview = service.overview()

        assertThat(overview.productName).isEqualTo("Infinity Traffic Control Center")
        assertThat(overview.corePurpose).contains("실시간 교통 이벤트")
        assertThat(overview.primaryUsers).contains("교통 관제 운영자")
        assertThat(overview.keyCapabilities).isNotEmpty()
        assertThat(overview.valueFlow).hasSize(4)
        assertThat(overview.valueFlow.map { it.order }).containsExactly(1, 2, 3, 4)
    }
}
