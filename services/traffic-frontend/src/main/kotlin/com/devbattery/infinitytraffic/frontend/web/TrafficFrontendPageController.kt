package com.devbattery.infinitytraffic.frontend.web

import com.devbattery.infinitytraffic.shared.contract.TrafficEventMessage
import com.devbattery.infinitytraffic.shared.contract.TrafficSummaryResponse
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Instant

/**
 * 타임리프 대시보드 페이지와 폼 액션을 담당한다.
 */
@Controller
@RequestMapping
class TrafficFrontendPageController(
    private val frontendGatewayClient: FrontendGatewayClient,
) {

    // 지역 선택 폼에서 사용할 기준 지역 목록을 제공한다.
    @ModelAttribute("regions")
    fun regions(): List<String> = listOf("ALL", "SEOUL", "BUSAN", "INCHEON", "DAEJEON", "GWANGJU")

    // 회원가입 폼 기본값을 모델에 제공한다.
    @ModelAttribute("registerForm")
    fun registerForm(): RegisterForm = RegisterForm()

    // 로그인 폼 기본값을 모델에 제공한다.
    @ModelAttribute("loginForm")
    fun loginForm(): LoginForm = LoginForm()

    // 이벤트 입력 폼 기본값을 모델에 제공한다.
    @ModelAttribute("trafficEventForm")
    fun trafficEventForm(): TrafficEventForm = TrafficEventForm()

    // 루트 경로를 대시보드로 리다이렉트한다.
    @GetMapping("/")
    fun root(): String = "redirect:/dashboard"

    // 대시보드를 렌더링하면서 요약/최근 이벤트를 초기 데이터로 주입한다.
    @GetMapping("/dashboard")
    fun dashboard(
        @RequestParam(required = false) region: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        model: Model,
        session: HttpSession,
    ): String {
        val safeRegion = normalizeRegion(region)
        val safeLimit = limit.coerceIn(5, 100)

        val summary = runCatching { frontendGatewayClient.summary(safeRegion) }
            .getOrElse { emptySummary() }

        val recentEvents = runCatching { frontendGatewayClient.recentEvents(safeLimit) }
            .getOrElse { emptyList() }

        val sessionSnapshot = sessionSnapshot(session)

        model.addAttribute("selectedRegion", safeRegion ?: "ALL")
        model.addAttribute("limit", safeLimit)
        model.addAttribute("summary", summary)
        model.addAttribute("recentEvents", recentEvents)
        model.addAttribute("authenticated", sessionSnapshot != null)
        model.addAttribute("username", sessionSnapshot?.username)
        model.addAttribute("tokenExpiresAt", sessionSnapshot?.expiresAt)
        model.addAttribute("gatewayBaseUrl", "/ui/api/dashboard")

        return "dashboard"
    }

    // 회원가입 폼을 처리하고 결과 메시지를 플래시로 전달한다.
    @PostMapping("/auth/register")
    fun register(
        @Valid @ModelAttribute("registerForm") form: RegisterForm,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes,
    ): String {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstValidationMessage(bindingResult))
            return "redirect:/dashboard"
        }

        return runCatching {
            frontendGatewayClient.register(form.toRequest())
        }.fold(
            onSuccess = {
                redirectAttributes.addFlashAttribute("successMessage", "${it.username} 계정이 생성되었습니다.")
                "redirect:/dashboard"
            },
            onFailure = {
                redirectAttributes.addFlashAttribute("errorMessage", it.message ?: "회원가입 처리 중 오류가 발생했습니다.")
                "redirect:/dashboard"
            },
        )
    }

    // 로그인 폼을 처리하고 세션에 인증 정보를 저장한다.
    @PostMapping("/auth/login")
    fun login(
        @Valid @ModelAttribute("loginForm") form: LoginForm,
        bindingResult: BindingResult,
        session: HttpSession,
        redirectAttributes: RedirectAttributes,
    ): String {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstValidationMessage(bindingResult))
            return "redirect:/dashboard"
        }

        return runCatching {
            val token = frontendGatewayClient.login(form.toRequest())
            val validation = frontendGatewayClient.validate(token.accessToken)
            if (!validation.valid) {
                throw FrontendGatewayException(statusCode = 401, message = "토큰 검증에 실패했습니다.")
            }

            val username = validation.username ?: form.username.trim()
            session.setAttribute(
                USER_SESSION_KEY,
                FrontendUserSession(username = username, accessToken = token.accessToken, expiresAt = token.expiresAt),
            )
        }.fold(
            onSuccess = {
                redirectAttributes.addFlashAttribute("successMessage", "로그인에 성공했습니다.")
                "redirect:/dashboard"
            },
            onFailure = {
                redirectAttributes.addFlashAttribute("errorMessage", it.message ?: "로그인 처리 중 오류가 발생했습니다.")
                "redirect:/dashboard"
            },
        )
    }

    // 현재 로그인 세션을 종료한다.
    @PostMapping("/auth/logout")
    fun logout(session: HttpSession, redirectAttributes: RedirectAttributes): String {
        session.removeAttribute(USER_SESSION_KEY)
        redirectAttributes.addFlashAttribute("successMessage", "로그아웃되었습니다.")
        return "redirect:/dashboard"
    }

    // 교통 이벤트 입력 폼을 처리해 커맨드 서비스로 전달한다.
    @PostMapping("/traffic/events")
    fun ingestTrafficEvent(
        @Valid @ModelAttribute("trafficEventForm") form: TrafficEventForm,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes,
    ): String {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstValidationMessage(bindingResult))
            return "redirect:/dashboard"
        }

        return runCatching {
            frontendGatewayClient.ingestTrafficEvent(form.toRequest())
        }.fold(
            onSuccess = {
                redirectAttributes.addFlashAttribute("successMessage", "이벤트가 수집되었습니다. eventId=${it.eventId}")
                "redirect:/dashboard"
            },
            onFailure = {
                redirectAttributes.addFlashAttribute("errorMessage", it.message ?: "이벤트 수집 중 오류가 발생했습니다.")
                "redirect:/dashboard"
            },
        )
    }

    // 세션 토큰이 유효할 때만 사용자 세션 정보를 반환한다.
    private fun sessionSnapshot(session: HttpSession): FrontendUserSession? {
        val saved = session.getAttribute(USER_SESSION_KEY) as? FrontendUserSession ?: return null
        return runCatching {
            val validation = frontendGatewayClient.validate(saved.accessToken)
            if (!validation.valid) {
                session.removeAttribute(USER_SESSION_KEY)
                null
            } else {
                val username = validation.username ?: saved.username
                FrontendUserSession(username = username, accessToken = saved.accessToken, expiresAt = saved.expiresAt)
            }
        }.getOrElse {
            session.removeAttribute(USER_SESSION_KEY)
            null
        }
    }

    // 검증 오류 메시지 중 첫 번째 메시지를 반환한다.
    private fun firstValidationMessage(bindingResult: BindingResult): String =
        bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "입력값을 확인해 주세요."

    // 지역 필터 문자열을 표준 값으로 보정한다.
    private fun normalizeRegion(region: String?): String? {
        val normalized = region?.trim()?.uppercase()
        return if (normalized.isNullOrBlank() || normalized == "ALL") {
            null
        } else {
            normalized
        }
    }

    // 대시보드 초기 렌더링 실패 시 사용할 빈 요약 응답을 만든다.
    private fun emptySummary(): TrafficSummaryResponse =
        TrafficSummaryResponse(
            generatedAt = Instant.now(),
            totalEvents = 0,
            regions = emptyList(),
        )

    companion object {
        const val USER_SESSION_KEY: String = "trafficFrontendUserSession"
    }
}
