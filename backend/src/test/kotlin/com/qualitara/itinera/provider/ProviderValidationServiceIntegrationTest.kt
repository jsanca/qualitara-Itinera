package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStateRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStatus
import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.internal.persistence.repository.OnboardingSessionRepository
import com.qualitara.itinera.internal.persistence.repository.OnboardingStepStateRepository
import com.qualitara.itinera.internal.persistence.repository.ProviderValidationAttemptRepository
import com.qualitara.itinera.workflow.AllowedAction
import com.qualitara.itinera.workflow.AllowedActionPolicy
import com.qualitara.itinera.workflow.CredentialFingerprint
import com.qualitara.itinera.workflow.OnboardingWorkflowService
import com.qualitara.itinera.workflow.ValidationStatus
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.payload.DetailsPayload
import com.qualitara.itinera.workflow.payload.ValidationPayload
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Integration tests for [ProviderValidationService].
 *
 * Requires a running PostgreSQL instance (docker compose up -d).
 * All tests clean up after themselves via [cleanup].
 */
@SpringBootTest
class ProviderValidationServiceIntegrationTest {

    @Autowired lateinit var validationService: ProviderValidationService
    @Autowired lateinit var workflowService: OnboardingWorkflowService
    @Autowired lateinit var sessionRepo: OnboardingSessionRepository
    @Autowired lateinit var stepStateRepo: OnboardingStepStateRepository
    @Autowired lateinit var attemptRepo: ProviderValidationAttemptRepository
    @Autowired lateinit var payloadMapper: JsonbPayloadMapper
    @Autowired lateinit var jdbc: NamedParameterJdbcTemplate

    @AfterEach
    fun cleanup() {
        jdbc.update("DELETE FROM partner_account", emptyMap<String, Any>())
        jdbc.update("DELETE FROM provider_validation_attempt", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_step_state", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_session", emptyMap<String, Any>())
    }

    /**
     * Creates a session at VALIDATION step with a DETAILS state using [accountId] as the trigger.
     * The fake Provider uses [accountId] to determine the outcome.
     * Optional [fingerprint] is stored in the payload to enable BR-001 reconstruction tests.
     */
    private fun createSession(accountId: String, fingerprint: String? = null): UUID {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.VALIDATION,
            status = OnboardingSessionStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        ))

        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = payloadMapper.toJsonString(DetailsPayload(
                companyName = "Test Corp",
                accountId = accountId,
                apiKeyPresent = true,
                apiKeyMasked = "****",
                credentialFingerprint = fingerprint
            )),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))

        return sessionId
    }

    private fun loadValidationPayload(sessionId: UUID): ValidationPayload {
        val record = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?: error("No validation step state found for $sessionId")
        return payloadMapper.fromJsonString(record.payloadJson, ValidationPayload::class.java)
    }

    // --- Required test scenarios ---

    @Test
    fun `valid result persists VALID payload with items and advances to REVIEW`() {
        val sessionId = createSession("valid")
        val finalState = validationService.validate(sessionId, "any-key")

        assertEquals(OnboardingStepKey.REVIEW, finalState.currentStep)
        assertEquals(ValidationStatus.VALID, finalState.validationStatus)

        val payload = loadValidationPayload(sessionId)
        assertEquals(ValidationStatus.VALID, payload.status)
        assertEquals(2, payload.items.size)
        assertTrue(payload.items.any { it.externalId == "feed-001" })
        assertTrue(payload.warnings.isEmpty())

        val session = sessionRepo.findById(sessionId)!!
        assertEquals(OnboardingStepKey.REVIEW, session.currentStep)
    }

    @Test
    fun `partial result persists items and warnings and advances to REVIEW`() {
        val sessionId = createSession("partial")
        val finalState = validationService.validate(sessionId, "any-key")

        assertEquals(OnboardingStepKey.REVIEW, finalState.currentStep)
        assertEquals(ValidationStatus.PARTIAL, finalState.validationStatus)

        val payload = loadValidationPayload(sessionId)
        assertEquals(ValidationStatus.PARTIAL, payload.status)
        assertEquals(1, payload.items.size)
        assertEquals(2, payload.warnings.size)
        assertTrue(payload.warnings.any { "Secondary Feed" in it })
    }

    @Test
    fun `invalid result persists reason and returns session to DETAILS`() {
        val sessionId = createSession("invalid")
        val finalState = validationService.validate(sessionId, "any-key")

        assertEquals(OnboardingStepKey.DETAILS, finalState.currentStep)
        assertEquals(ValidationStatus.INVALID, finalState.validationStatus)

        val payload = loadValidationPayload(sessionId)
        assertEquals(ValidationStatus.INVALID, payload.status)
        assertNotNull(payload.reason)

        val session = sessionRepo.findById(sessionId)!!
        assertEquals(OnboardingStepKey.DETAILS, session.currentStep)
    }

    @Test
    fun `unavailable result is retryable and stays in VALIDATION`() {
        val sessionId = createSession("unavailable")
        val finalState = validationService.validate(sessionId, "any-key")

        assertEquals(OnboardingStepKey.VALIDATION, finalState.currentStep)
        assertEquals(ValidationStatus.UNAVAILABLE, finalState.validationStatus)
        assertTrue(AllowedAction.RETRY_VALIDATION in AllowedActionPolicy.resolve(finalState))

        val session = sessionRepo.findById(sessionId)!!
        assertEquals(OnboardingStepKey.VALIDATION, session.currentStep)

        val payload = loadValidationPayload(sessionId)
        assertEquals(ValidationStatus.UNAVAILABLE, payload.status)
    }

    @Test
    fun `timeout result is retryable and stays in VALIDATION`() {
        val sessionId = createSession("timeout")
        val finalState = validationService.validate(sessionId, "any-key")

        assertEquals(OnboardingStepKey.VALIDATION, finalState.currentStep)
        assertEquals(ValidationStatus.TIMEOUT, finalState.validationStatus)
        assertTrue(AllowedAction.RETRY_VALIDATION in AllowedActionPolicy.resolve(finalState))

        val payload = loadValidationPayload(sessionId)
        assertEquals(ValidationStatus.TIMEOUT, payload.status)

        val attempts = attemptRepo.findBySessionIdOrderByAttemptNumber(sessionId)
        assertEquals(1, attempts.size)
        assertNotNull(attempts.first().errorMessage)
        assertNull(attempts.first().responsePayloadJson)
    }

    @Test
    fun `validation attempts are auditable and ordered`() {
        val sessionId = createSession("unavailable")

        validationService.validate(sessionId, "any-key") // attempt 1
        validationService.validate(sessionId, "any-key") // attempt 2
        validationService.validate(sessionId, "any-key") // attempt 3

        val attempts = attemptRepo.findBySessionIdOrderByAttemptNumber(sessionId)
        assertEquals(3, attempts.size)
        assertEquals(listOf(1, 2, 3), attempts.map { it.attemptNumber })
        assertTrue(attempts.all { it.outcome == ProviderValidationOutcome.UNAVAILABLE })
        assertTrue(attempts.all { it.accountId == "unavailable" })
        assertTrue(attempts.all { it.startedAt != null && it.completedAt != null })
    }

    @Test
    fun `retry creates another attempt record but only one latest validation state`() {
        val sessionId = createSession("unavailable")

        validationService.validate(sessionId, "any-key")
        validationService.validate(sessionId, "any-key")

        val attempts = attemptRepo.findBySessionIdOrderByAttemptNumber(sessionId)
        assertEquals(2, attempts.size, "Two retries must produce two distinct attempt records")

        val validationRows = stepStateRepo.findAllBySessionId(sessionId)
            .filter { it.stepKey == OnboardingStepKey.VALIDATION }
        assertEquals(1, validationRows.size, "Only one latest validation state row must exist")

        val payload = payloadMapper.fromJsonString(validationRows.first().payloadJson, ValidationPayload::class.java)
        assertEquals(ValidationStatus.UNAVAILABLE, payload.status)
    }

    @Test
    fun `validate throws when session does not exist`() {
        assertThrows<InvalidWorkflowTransitionException> {
            validationService.validate(UUID.randomUUID(), "any-key")
        }
    }

    @Test
    fun `validate throws when details have not been submitted`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.VALIDATION,
            status = OnboardingSessionStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        ))

        assertThrows<InvalidWorkflowTransitionException> {
            validationService.validate(sessionId, "any-key")
        }
    }

    @Test
    fun `attempt record stores request fingerprint and not the raw API key`() {
        val sessionId = createSession("valid")
        val apiKey = "super-secret-key"
        validationService.validate(sessionId, apiKey)

        val attempt = attemptRepo.findBySessionIdOrderByAttemptNumber(sessionId).first()
        assertFalse(attempt.requestFingerprint.contains(apiKey), "Raw API key must not appear in fingerprint")
        assertEquals(64, attempt.requestFingerprint.length, "SHA-256 hex digest is 64 chars")
        assertNull(attempt.errorMessage)
        assertNotNull(attempt.responsePayloadJson)
    }

    @Test
    fun `BR-001 credential fingerprint survives round-trip and triggers STALE on credential change`() {
        // Simulate details submission: store a fingerprint as if DetailsService had computed it
        val storedFingerprint = CredentialFingerprint.compute("valid", "original-key")
        val sessionId = createSession("valid", fingerprint = storedFingerprint)

        // validate() reads the fingerprint from DetailsPayload and wires it into WorkflowSessionState
        val finalState = validationService.validate(sessionId, "any-key")
        assertEquals(ValidationStatus.VALID, finalState.validationStatus)
        assertEquals(storedFingerprint, finalState.credentialFingerprint,
            "Reconstructed state must carry the fingerprint loaded from DetailsPayload")

        // Simulate a credential change: different apiKey produces a different fingerprint
        val newFingerprint = CredentialFingerprint.compute("valid", "new-key")

        // BR-001: applyDetailsSubmission detects the fingerprint change and marks validation STALE
        val staleState = workflowService.applyDetailsSubmission(finalState, newFingerprint)
        assertEquals(ValidationStatus.STALE, staleState.validationStatus,
            "Changing credentials after a valid result must invalidate validation (BR-001)")
    }
}
