package com.devbattery.infinitytraffic.frontend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 프론트엔드가 노출할 모니터링 도구 URL과 헬스 체크 대상을 보관한다.
 */
@ConfigurationProperties(prefix = "frontend.monitoring")
data class FrontendMonitoringProperties(
    val prometheusUrl: String = "http://localhost:9091",
    val grafanaUrl: String = "http://localhost:3000",
    val grafanaDashboardPath: String = "/d/infinity-traffic-overview/infinity-traffic-overview?orgId=1&refresh=5s",
    val targets: List<MonitoringTarget> = defaultTargets(),
) {

    data class MonitoringTarget(
        val name: String,
        val healthUrl: String,
    )

    companion object {
        fun defaultTargets(): List<MonitoringTarget> =
            listOf(
                MonitoringTarget(name = "api-gateway", healthUrl = "http://localhost:8080/actuator/health"),
                MonitoringTarget(name = "auth-service", healthUrl = "http://localhost:8081/actuator/health"),
                MonitoringTarget(name = "traffic-command-service", healthUrl = "http://localhost:8082/actuator/health"),
                MonitoringTarget(name = "traffic-query-service", healthUrl = "http://localhost:8083/actuator/health"),
                MonitoringTarget(name = "traffic-frontend", healthUrl = "http://localhost:8084/actuator/health"),
            )
    }
}
