package com.qualitara.itinera.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

/**
 * End-to-end integration tests for the onboarding REST API.
 *
 * Exercises the full stack from HTTP through application service, domain services,
 * and repositories to PostgreSQL. Requires a running database (docker compose up -d).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OnboardingApiIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var jdbc: NamedParameterJdbcTemplate

    @AfterEach
    fun cleanup() {
        jdbc.update("DELETE FROM partner_account", emptyMap<String, Any>())
        jdbc.update("DELETE FROM provider_validation_attempt", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_step_state", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_session", emptyMap<String, Any>())
    }

    // --- Helpers ---

    private fun createSession(): String {
        val result = mockMvc.post("/api/onboarding/sessions")
            .andExpect { status { isCreated() } }
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("sessionId").asText()
    }

    private fun submitDetails(sessionId: String, accountId: String = "valid", apiKey: String = "secret-key"): String {
        val result = mockMvc.put("/api/onboarding/sessions/$sessionId/details") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"companyName":"Acme Corp","accountId":"$accountId","apiKey":"$apiKey"}"""
        }.andExpect { status { isOk() } }.andReturn()
        return result.response.contentAsString
    }

    private fun triggerValidation(sessionId: String, apiKey: String = "secret-key"): String {
        val result = mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"apiKey":"$apiKey"}"""
        }.andExpect { status { isOk() } }.andReturn()
        return result.response.contentAsString
    }

    // --- Tests ---

    @Test
    fun `create session returns DETAILS step, DRAFT status, NOT_STARTED validation, SUBMIT_DETAILS action`() {
        mockMvc.post("/api/onboarding/sessions")
            .andExpect {
                status { isCreated() }
                jsonPath("$.currentStep") { value("DETAILS") }
                jsonPath("$.sessionStatus") { value("DRAFT") }
                jsonPath("$.validationStatus") { value("NOT_STARTED") }
                jsonPath("$.details") { isEmpty() }
                jsonPath("$.validation.status") { value("NOT_STARTED") }
                jsonPath("$.validation.items") { isArray() }
                jsonPath("$.allowedActions[0]") { value("SUBMIT_DETAILS") }
            }
    }

    @Test
    fun `create session sets Location header`() {
        val result = mockMvc.post("/api/onboarding/sessions")
            .andExpect { status { isCreated() } }
            .andReturn()
        val sessionId = objectMapper.readTree(result.response.contentAsString).get("sessionId").asText()
        val location = result.response.getHeader("Location")
        assert(location == "/api/onboarding/sessions/$sessionId") {
            "Expected Location=/api/onboarding/sessions/$sessionId but got $location"
        }
    }

    @Test
    fun `get session returns persisted state`() {
        val sessionId = createSession()
        mockMvc.get("/api/onboarding/sessions/$sessionId")
            .andExpect {
                status { isOk() }
                jsonPath("$.sessionId") { value(sessionId) }
                jsonPath("$.currentStep") { value("DETAILS") }
            }
    }

    @Test
    fun `submit details stores redacted details and never returns raw apiKey`() {
        val sessionId = createSession()
        val responseJson = submitDetails(sessionId, apiKey = "super-secret-key")
        val tree = objectMapper.readTree(responseJson)

        assert(!responseJson.contains("super-secret-key")) { "Raw API key must not appear in response" }
        assert(tree["details"]["apiKeyPresent"].asBoolean()) { "apiKeyPresent must be true" }
        assert(tree["details"]["apiKeyMasked"].asText().endsWith("-key")) { "Masked key shows last 4 chars" }
        assert(!tree["details"].has("credentialFingerprint")) { "Fingerprint must not appear in response" }
    }

    @Test
    fun `submit details advances step to VALIDATION`() {
        val sessionId = createSession()
        submitDetails(sessionId)
        mockMvc.get("/api/onboarding/sessions/$sessionId")
            .andExpect {
                status { isOk() }
                jsonPath("$.currentStep") { value("VALIDATION") }
            }
    }

    @Test
    fun `submit details after valid validation with changed credentials returns STALE`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "valid", apiKey = "original-key")
        triggerValidation(sessionId, apiKey = "original-key")

        val staleResponse = submitDetails(sessionId, accountId = "valid", apiKey = "changed-key")
        val tree = objectMapper.readTree(staleResponse)

        assert(tree["validationStatus"].asText() == "STALE") { "BR-001: changed credentials must mark validation STALE" }
        assert(tree["currentStep"].asText() == "VALIDATION") { "Session must stay in VALIDATION step" }
    }

    @Test
    fun `validation endpoint handles valid outcome and advances to REVIEW`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "valid")

        mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"apiKey":"any-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.currentStep") { value("REVIEW") }
            jsonPath("$.validationStatus") { value("VALID") }
            jsonPath("$.validation.items[0].externalId") { value("feed-001") }
        }
    }

    @Test
    fun `validation endpoint handles partial outcome and returns items and warnings`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "partial")
        triggerValidation(sessionId)

        mockMvc.get("/api/onboarding/sessions/$sessionId")
            .andExpect {
                status { isOk() }
                jsonPath("$.validationStatus") { value("PARTIAL") }
                jsonPath("$.validation.items") { isNotEmpty() }
                jsonPath("$.validation.warnings") { isNotEmpty() }
            }
    }

    @Test
    fun `validation endpoint handles invalid outcome and returns session to DETAILS`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "invalid")

        mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"apiKey":"any-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.currentStep") { value("DETAILS") }
            jsonPath("$.validationStatus") { value("INVALID") }
            jsonPath("$.validation.reason") { isNotEmpty() }
        }
    }

    @Test
    fun `validation endpoint handles unavailable outcome and stays in VALIDATION with retry action`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "unavailable")

        mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"apiKey":"any-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.currentStep") { value("VALIDATION") }
            jsonPath("$.validationStatus") { value("UNAVAILABLE") }
            jsonPath("$.allowedActions[?(@ == 'RETRY_VALIDATION')]") { isNotEmpty() }
        }
    }

    @Test
    fun `validation endpoint handles timeout outcome and stays in VALIDATION with retry action`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "timeout")

        mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"apiKey":"any-key"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.currentStep") { value("VALIDATION") }
            jsonPath("$.validationStatus") { value("TIMEOUT") }
            jsonPath("$.allowedActions[?(@ == 'RETRY_VALIDATION')]") { isNotEmpty() }
        }
    }

    @Test
    fun `go-live succeeds after valid validation`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "valid")
        triggerValidation(sessionId)

        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isOk() }
                jsonPath("$.currentStep") { value("COMPLETE") }
                jsonPath("$.sessionStatus") { value("LIVE") }
                jsonPath("$.validationStatus") { value("VALID") }
                jsonPath("$.allowedActions") { isEmpty() }
            }
    }

    @Test
    fun `go-live succeeds after partial validation`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "partial")
        triggerValidation(sessionId)

        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isOk() }
                jsonPath("$.currentStep") { value("COMPLETE") }
                jsonPath("$.sessionStatus") { value("LIVE") }
                jsonPath("$.validationStatus") { value("PARTIAL") }
            }
    }

    @Test
    fun `unavailable retry flow — unavailable response includes retry action, retry with valid credentials succeeds, go-live works`() {
        val sessionId = createSession()

        // Step 1: submit details with unavailable accountId and validate → UNAVAILABLE
        submitDetails(sessionId, accountId = "unavailable", apiKey = "unavail-key")
        val unavailableResponse = triggerValidation(sessionId, apiKey = "unavail-key")
        val unavailableTree = objectMapper.readTree(unavailableResponse)

        assert(unavailableTree["validationStatus"].asText() == "UNAVAILABLE") { "First validation must be UNAVAILABLE" }
        assert(unavailableTree["allowedActions"].find { it.asText() == "RETRY_VALIDATION" } != null) {
            "UNAVAILABLE response must include RETRY_VALIDATION action"
        }

        // Step 2: re-submit with valid accountId (credentials changed → BR-001: STALE)
        val staleResponse = submitDetails(sessionId, accountId = "valid", apiKey = "new-key")
        val staleTree = objectMapper.readTree(staleResponse)
        assert(staleTree["validationStatus"].asText() == "STALE") { "Changed credentials must mark validation STALE" }
        assert(staleTree["currentStep"].asText() == "VALIDATION") { "Must stay in VALIDATION after STALE" }

        // Step 3: retry validation with new credentials → VALID
        val validResponse = triggerValidation(sessionId, apiKey = "new-key")
        val validTree = objectMapper.readTree(validResponse)
        assert(validTree["validationStatus"].asText() == "VALID") { "Retry with valid credentials must succeed" }
        assert(validTree["currentStep"].asText() == "REVIEW") { "Must advance to REVIEW after VALID" }

        // Step 4: go-live succeeds
        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isOk() }
                jsonPath("$.currentStep") { value("COMPLETE") }
                jsonPath("$.sessionStatus") { value("LIVE") }
                jsonPath("$.validationStatus") { value("VALID") }
                jsonPath("$.allowedActions") { isEmpty() }
            }
    }

    @Test
    fun `go-live rejected before validation succeeds returns 409`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "invalid")
        triggerValidation(sessionId)

        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("INVALID_TRANSITION") }
            }
    }

    @Test
    fun `repeated go-live is idempotent`() {
        val sessionId = createSession()
        submitDetails(sessionId, accountId = "valid")
        triggerValidation(sessionId)
        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect { status { isOk() } }

        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isOk() }
                jsonPath("$.currentStep") { value("COMPLETE") }
                jsonPath("$.sessionStatus") { value("LIVE") }
            }

        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM partner_account WHERE session_id = :sessionId",
            mapOf("sessionId" to UUID.fromString(sessionId)),
            Int::class.java
        )
        assert(count == 1) { "Exactly one partner account must exist after idempotent go-live" }
    }

    @Test
    fun `unknown session returns 404 with SESSION_NOT_FOUND code`() {
        mockMvc.get("/api/onboarding/sessions/${UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("SESSION_NOT_FOUND") }
            }
    }

    @Test
    fun `invalid transition returns 409 with INVALID_TRANSITION code and safe message`() {
        val sessionId = createSession()
        mockMvc.post("/api/onboarding/sessions/$sessionId/go-live")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("INVALID_TRANSITION") }
                jsonPath("$.message") { isNotEmpty() }
            }
    }

    @Test
    fun `missing required field in submit details returns 400 with MALFORMED_REQUEST code`() {
        val sessionId = createSession()
        mockMvc.put("/api/onboarding/sessions/$sessionId/details") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"companyName":"Acme"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
        }
    }

    @Test
    fun `missing request body for validation returns 400 with MALFORMED_REQUEST code`() {
        val sessionId = createSession()
        submitDetails(sessionId)
        mockMvc.post("/api/onboarding/sessions/$sessionId/validation") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
        }
    }
}
