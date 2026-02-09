package com.devbattery.infinitytraffic.shared.tracing

import java.util.UUID

/**
 * 서비스 간 요청 추적 시 사용하는 공통 헤더 이름을 정의한다.
 */
object TraceHeaders {
    const val TRACE_ID: String = "X-Trace-Id"
}

/**
 * 분산 추적에 사용할 고유 추적 ID를 생성한다.
 */
object TraceIdGenerator {
    // 요청 단위로 고유한 Trace ID를 발급한다.
    fun create(): String = UUID.randomUUID().toString().replace("-", "")
}
