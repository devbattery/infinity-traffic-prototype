package com.devbattery.infinitytraffic.frontend.service

import com.devbattery.infinitytraffic.frontend.config.FrontendMonitoringProperties
import com.devbattery.infinitytraffic.frontend.web.MonitoringLink
import com.devbattery.infinitytraffic.frontend.web.MonitoringSnapshotResponse
import com.devbattery.infinitytraffic.frontend.web.MonitoringTargetStatus
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Instant

/**
 * 운영 관점에서 필요한 모니터링 링크와 서비스 헬스 스냅샷을 제공한다.
 */
@Service
class FrontendMonitoringService(
    restClientBuilder: RestClient.Builder,
    private val monitoringProperties: FrontendMonitoringProperties,
    private val objectMapper: ObjectMapper,
) {

    private val restClient: RestClient = restClientBuilder.build()

    // UI가 한 번에 렌더링할 수 있도록 링크/헬스 상태를 단일 응답으로 반환한다.
    fun snapshot(): MonitoringSnapshotResponse =
        MonitoringSnapshotResponse(
            generatedAt = Instant.now(),
            links =
                listOf(
                    MonitoringLink(
                        name = "Prometheus",
                        url = monitoringProperties.prometheusUrl,
                        description = "원시 메트릭 조회 및 PromQL 분석",
                    ),
                    MonitoringLink(
                        name = "Grafana",
                        url = monitoringProperties.grafanaUrl,
                        description = "통합 운영 대시보드",
                    ),
                    MonitoringLink(
                        name = "Grafana Dashboard",
                        url = joinUrl(monitoringProperties.grafanaUrl, monitoringProperties.grafanaDashboardPath),
                        description = "Infinity Traffic 사전 구성 대시보드",
                    ),
                ),
            targets = monitoringProperties.targets.map { checkTargetHealth(it) },
        )

    // 각 서비스의 health endpoint를 점검해 상태/응답시간을 계산한다.
    private fun checkTargetHealth(target: FrontendMonitoringProperties.MonitoringTarget): MonitoringTargetStatus {
        val startedAtNanos = System.nanoTime()

        return runCatching {
            val rawBody =
                restClient.get()
                    .uri(target.healthUrl)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                    .retrieve()
                    .body(String::class.java)
                    .orEmpty()

            val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000
            val status = extractStatus(rawBody)

            MonitoringTargetStatus(
                name = target.name,
                healthUrl = target.healthUrl,
                status = status,
                responseTimeMs = elapsedMillis,
                detail = extractDetail(rawBody),
                checkedAt = Instant.now(),
            )
        }.getOrElse { error ->
            MonitoringTargetStatus(
                name = target.name,
                healthUrl = target.healthUrl,
                status = "DOWN",
                responseTimeMs = null,
                detail = error.message?.trim()?.take(180) ?: "헬스 체크 호출에 실패했습니다.",
                checkedAt = Instant.now(),
            )
        }
    }

    // Spring Actuator 또는 Grafana 스타일 응답에서 상태 필드를 해석한다.
    private fun extractStatus(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "UP"
        }

        return runCatching {
            val node = objectMapper.readTree(rawBody)
            parseStatusFromNode(node)
        }.getOrElse {
            "UP"
        }
    }

    // 상태 필드가 없더라도 2xx 응답이면 가용으로 간주한다.
    private fun parseStatusFromNode(node: JsonNode): String {
        val statusText = node.path("status").asText("").trim()
        if (statusText.isNotBlank()) {
            return statusText.uppercase()
        }

        val databaseText = node.path("database").asText("").trim()
        if (databaseText.equals("ok", ignoreCase = true)) {
            return "UP"
        }

        return "UP"
    }

    // 디버깅에 필요한 핵심 정보만 요약해 UI에 제공한다.
    private fun extractDetail(rawBody: String): String {
        if (rawBody.isBlank()) {
            return "HTTP 200 응답"
        }

        return rawBody
            .replace("\\s+".toRegex(), " ")
            .trim()
            .take(180)
    }

    // URL 결합 규칙을 통일해 환경별(base-url) 차이를 흡수한다.
    private fun joinUrl(baseUrl: String, path: String): String {
        val trimmedPath = path.trim()
        if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) {
            return trimmedPath
        }

        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = if (trimmedPath.startsWith("/")) trimmedPath else "/$trimmedPath"
        return "$normalizedBase$normalizedPath"
    }
}
