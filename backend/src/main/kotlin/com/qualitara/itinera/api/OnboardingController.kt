package com.qualitara.itinera.api

import com.qualitara.itinera.api.dto.SessionResponse
import com.qualitara.itinera.api.dto.SubmitDetailsRequest
import com.qualitara.itinera.api.dto.TriggerValidationRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * REST controller for the onboarding wizard API.
 *
 * Delegates all orchestration to [OnboardingApplicationService].
 * This controller is intentionally thin: it handles HTTP concerns only
 * (path variables, request bodies, response status, Location header).
 */
@RestController
@RequestMapping("/api/onboarding/sessions")
class OnboardingController(private val appService: OnboardingApplicationService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(response: HttpServletResponse): SessionResponse {
        val session = appService.createSession()
        response.addHeader("Location", "/api/onboarding/sessions/${session.sessionId}")
        return session
    }

    @GetMapping("/{sessionId}")
    fun getSession(@PathVariable sessionId: UUID): SessionResponse =
        appService.getSession(sessionId)

    @PutMapping("/{sessionId}/details")
    fun submitDetails(
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: SubmitDetailsRequest
    ): SessionResponse = appService.submitDetails(sessionId, request)

    @PostMapping("/{sessionId}/validation")
    fun triggerValidation(
        @PathVariable sessionId: UUID,
        @Valid @RequestBody request: TriggerValidationRequest
    ): SessionResponse = appService.triggerValidation(sessionId, request.apiKey)

    @PostMapping("/{sessionId}/go-live")
    fun goLive(@PathVariable sessionId: UUID): SessionResponse =
        appService.goLive(sessionId)
}
