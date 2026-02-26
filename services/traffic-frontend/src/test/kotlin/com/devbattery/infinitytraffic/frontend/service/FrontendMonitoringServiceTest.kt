package com.devbattery.infinitytraffic.frontend.service

import com.devbattery.infinitytraffic.frontend.config.FrontendMonitoringProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

/**
 * FrontendMonitoringService 단위 테스트다.
 */
class FrontendMonitoringServiceTest {

    private lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.path) {
                        "/actuator/health/up" ->
                            MockResponse()
                                .setResponseCode(200)
                                .addHeader("Content-Type", "application/json")
                                .setBody("""{"status":"UP"}""")

                        "/actuator/health/down" ->
                            MockResponse()
                                .setResponseCode(500)
                                .addHeader("Content-Type", "application/json")
                                .setBody("""{"status":"DOWN"}""")

                        else ->
                            MockResponse()
                                .setResponseCode(404)
                                .addHeader("Content-Type", "application/json")
                                .setBody("""{"message":"not found"}""")
                    }
                }
            }
        mockServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    // 모니터링 스냅샷이 링크와 타깃 상태를 함께 반환하는지 검증한다.
    @Test
    fun snapshotShouldContainLinksAndTargetHealth() {
        val base = mockServer.url("/").toString().removeSuffix("/")
        val properties =
            FrontendMonitoringProperties(
                prometheusUrl = "http://localhost:9091",
                grafanaUrl = "http://localhost:3000",
                grafanaDashboardPath = "/d/infinity-traffic-overview/infinity-traffic-overview?orgId=1",
                targets =
                    listOf(
                        FrontendMonitoringProperties.MonitoringTarget(
                            name = "api-gateway",
                            healthUrl = "$base/actuator/health/up",
                        ),
                    ),
            )
        val service =
            FrontendMonitoringService(
                restClientBuilder = RestClient.builder(),
                monitoringProperties = properties,
                objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            )

        val snapshot = service.snapshot()

        assertThat(snapshot.links).hasSize(3)
        assertThat(snapshot.links.map { it.name }).contains("Prometheus", "Grafana", "Grafana Dashboard")
        assertThat(snapshot.links.find { it.name == "Grafana Dashboard" }?.url)
            .isEqualTo("http://localhost:3000/d/infinity-traffic-overview/infinity-traffic-overview?orgId=1")

        assertThat(snapshot.targets).hasSize(1)
        val target = snapshot.targets.first()
        assertThat(target.name).isEqualTo("api-gateway")
        assertThat(target.status).isEqualTo("UP")
        assertThat(target.responseTimeMs).isNotNull
    }

    // 헬스 체크가 실패한 타깃은 DOWN으로 반환되는지 검증한다.
    @Test
    fun snapshotShouldMarkTargetDownWhenHealthEndpointFails() {
        val base = mockServer.url("/").toString().removeSuffix("/")
        val properties =
            FrontendMonitoringProperties(
                prometheusUrl = "http://localhost:9091",
                grafanaUrl = "http://localhost:3000",
                grafanaDashboardPath = "/d/infinity-traffic-overview/infinity-traffic-overview?orgId=1",
                targets =
                    listOf(
                        FrontendMonitoringProperties.MonitoringTarget(
                            name = "traffic-query-service",
                            healthUrl = "$base/actuator/health/down",
                        ),
                    ),
            )
        val service =
            FrontendMonitoringService(
                restClientBuilder = RestClient.builder(),
                monitoringProperties = properties,
                objectMapper = jacksonObjectMapper().findAndRegisterModules(),
            )

        val snapshot = service.snapshot()

        assertThat(snapshot.targets).hasSize(1)
        val target = snapshot.targets.first()
        assertThat(target.name).isEqualTo("traffic-query-service")
        assertThat(target.status).isEqualTo("DOWN")
        assertThat(target.detail).isNotBlank
    }
}
