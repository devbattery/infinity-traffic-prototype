package com.devbattery.infinitytraffic.command.web

import com.devbattery.infinitytraffic.shared.contract.ApiError
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.devbattery.infinitytraffic.shared.tracing.TraceIdGenerator
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 커맨드 서비스 예외를 표준 에러 포맷으로 변환한다.
 */
@RestControllerAdvice
class TrafficCommandExceptionHandler {

    // 요청 검증 예외를 처리한다.
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val message = exception.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(buildError("VALIDATION_ERROR", message, request))
    }

    // 잘못된 요청 예외를 처리한다.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(
            buildError("BAD_REQUEST", exception.message ?: "요청이 올바르지 않습니다.", request),
        )
    }

    // Kafka 발행 등 내부 오류를 처리한다.
    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            buildError("INTERNAL_SERVER_ERROR", "내부 서버 오류가 발생했습니다.", request),
        )
    }

    // 공통 에러 응답 객체를 생성한다.
    private fun buildError(code: String, message: String, request: HttpServletRequest): ApiError {
        val traceId = request.getHeader(TraceHeaders.TRACE_ID) ?: TraceIdGenerator.create()
        return ApiError(code = code, message = message, traceId = traceId)
    }
}
