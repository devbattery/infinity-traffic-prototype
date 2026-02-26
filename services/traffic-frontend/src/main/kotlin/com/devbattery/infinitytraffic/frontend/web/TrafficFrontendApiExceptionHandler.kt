package com.devbattery.infinitytraffic.frontend.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 프론트엔드 JSON API 전용 예외를 표준 오류 응답으로 변환한다.
 */
@RestControllerAdvice(assignableTypes = [TrafficFrontendApiController::class])
class TrafficFrontendApiExceptionHandler {

    // 게이트웨이 호출 실패를 HTTP 상태코드와 함께 변환한다.
    @ExceptionHandler(FrontendGatewayException::class)
    fun handleGatewayException(exception: FrontendGatewayException): ResponseEntity<FrontendApiError> {
        val status = HttpStatus.resolve(exception.statusCode) ?: HttpStatus.BAD_GATEWAY
        return ResponseEntity
            .status(status)
            .body(FrontendApiError(code = "GATEWAY_ERROR", message = exception.message))
    }

    // 요청 바인딩 검증 실패를 400 오류로 반환한다.
    @ExceptionHandler(MethodArgumentNotValidException::class, BindException::class)
    fun handleValidationException(exception: Exception): ResponseEntity<FrontendApiError> {
        val message =
            when (exception) {
                is MethodArgumentNotValidException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
                is BindException -> exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
                else -> null
            } ?: "입력값이 유효하지 않습니다."

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(FrontendApiError(code = "VALIDATION_ERROR", message = message))
    }

    // 처리되지 않은 예외를 500 오류로 반환한다.
    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(exception: Exception): ResponseEntity<FrontendApiError> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(FrontendApiError(code = "INTERNAL_ERROR", message = exception.message ?: "서버 오류가 발생했습니다."))
    }
}
