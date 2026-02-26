package com.devbattery.infinitytraffic.frontend.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * SPA 라우트 진입 경로를 정적 index.html로 포워딩한다.
 */
@Controller
class TrafficFrontendSpaController {

    // 루트 및 대시보드 경로를 SPA 엔트리 파일로 연결한다.
    @GetMapping("/", "/dashboard")
    fun index(): String = "forward:/index.html"
}
