package com.devbattery.infinitytraffic.auth.web

import com.devbattery.infinitytraffic.shared.contract.ApiError
import com.devbattery.infinitytraffic.shared.tracing.TraceHeaders
import com.devbattery.infinitytraffic.shared.tracing.TraceIdGenerator
import io.jsonwebtoken.JwtException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 인증 서비스의 예외를 표준 API 에러 포맷으로 변환한다.
 */
@RestControllerAdvice
class AuthExceptionHandler {

    // 잘못된 요청 데이터 예외를 처리한다.
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val message = exception.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(
            buildError(
                code = "VALIDATION_ERROR",
                message = message,
                request = request,
            ),
        )
    }

    // 도메인 유효성 예외를 처리한다.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(
            buildError(
                code = "BAD_REQUEST",
                message = exception.message ?: "요청이 올바르지 않습니다.",
                request = request,
            ),
        )
    }

    // JWT 파싱/검증 실패 예외를 처리한다.
    @ExceptionHandler(JwtException::class)
    fun handleJwtException(
        exception: JwtException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            buildError(
                code = "UNAUTHORIZED",
                message = "유효하지 않은 토큰입니다.",
                request = request,
            ),
        )
    }

    // 기타 예외를 처리한다.
    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            buildError(
                code = "INTERNAL_SERVER_ERROR",
                message = "내부 서버 오류가 발생했습니다.",
                request = request,
            ),
        )
    }

    // 표준 에러 응답 객체를 생성한다.
    private fun buildError(
        code: String,
        message: String,
        request: HttpServletRequest,
    ): ApiError {
        val traceId = request.getHeader(TraceHeaders.TRACE_ID) ?: TraceIdGenerator.create()
        return ApiError(code = code, message = message, traceId = traceId)
    }
}
