package com.qualitara.itinera.golive

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStateRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStatus
import com.qualitara.itinera.internal.persistence.repository.OnboardingSessionRepository
import com.qualitara.itinera.internal.persistence.repository.OnboardingStepStateRepository
import com.qualitara.itinera.internal.persistence.repository.PartnerAccountRepository
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
 * Integration tests for [GoLiveService].
 *
 * Requires a running PostgreSQL instance (docker compose up -d).
 * All tests clean up after themselves via [cleanup].
 */
@SpringBootTest
class GoLiveServiceIntegrationTest {

    @Autowired lateinit var goLiveService: GoLiveService
    @Autowired lateinit var sessionRepo: OnboardingSessionRepository
    @Autowired lateinit var stepStateRepo: OnboardingStepStateRepository
    @Autowired lateinit var partnerAccountRepo: PartnerAccountRepository
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
     * Creates a session at REVIEW step with the given [validationStatus] in the VALIDATION payload.
     * [companyName] is stored in the DETAILS payload and used when creating the partner account.
     */
    private fun createReviewSession(
        validationStatus: ValidationStatus,
        companyName: String = "Test Corp"
    ): UUID {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.REVIEW,
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
                companyName = companyName,
                accountId = "test-account",
                apiKeyPresent = true,
                apiKeyMasked = "****"
            )),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))

        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            stepKey = OnboardingStepKey.VALIDATION,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = payloadMapper.toJsonString(ValidationPayload(status = validationStatus)),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))

        return sessionId
    }

    @Test
    fun `valid validation advances session to COMPLETE and marks it LIVE`() {
        val sessionId = createReviewSession(ValidationStatus.VALID)
        val finalState = goLiveService.goLive(sessionId)

        assertEquals(OnboardingStepKey.COMPLETE, finalState.currentStep)
        assertEquals(ValidationStatus.VALID, finalState.validationStatus)

        val session = sessionRepo.findById(sessionId)!!
        assertEquals(OnboardingStepKey.COMPLETE, session.currentStep)
        assertEquals(OnboardingSessionStatus.LIVE, session.status)
        assertNotNull(session.completedAt)
    }

    @Test
    fun `partial validation can go live`() {
        val sessionId = createReviewSession(ValidationStatus.PARTIAL)
        val finalState = goLiveService.goLive(sessionId)

        assertEquals(OnboardingStepKey.COMPLETE, finalState.currentStep)
        assertEquals(ValidationStatus.PARTIAL, finalState.validationStatus)

        val session = sessionRepo.findById(sessionId)!!
        assertEquals(OnboardingSessionStatus.LIVE, session.status)
    }

    @Test
    fun `go-live creates exactly one partner account with company name from details`() {
        val sessionId = createReviewSession(ValidationStatus.VALID, companyName = "Acme Corp")
        goLiveService.goLive(sessionId)

        val account = partnerAccountRepo.findBySessionId(sessionId)
        assertNotNull(account)
        assertEquals("Acme Corp", account!!.companyName)
        assertEquals(sessionId, account.sessionId)
        assertNotNull(account.wentLiveAt)
    }

    @Test
    fun `go-live twice does not create a duplicate partner account`() {
        val sessionId = createReviewSession(ValidationStatus.VALID)
        goLiveService.goLive(sessionId)
        goLiveService.goLive(sessionId) // idempotent second call

        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM partner_account WHERE session_id = :sessionId",
            mapOf("sessionId" to sessionId),
            Int::class.java
        )
        assertEquals(1, count, "Exactly one partner account must exist after two go-live calls")
    }

    @Test
    fun `repeated go-live on already-live session returns completed state`() {
        val sessionId = createReviewSession(ValidationStatus.VALID)
        goLiveService.goLive(sessionId)

        val idempotentState = goLiveService.goLive(sessionId)
        assertEquals(OnboardingStepKey.COMPLETE, idempotentState.currentStep)
        assertEquals(ValidationStatus.VALID, idempotentState.validationStatus)
    }

    @Test
    fun `go-live rejected when validation is INVALID`() {
        val sessionId = createReviewSession(ValidationStatus.INVALID)
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when validation is UNAVAILABLE`() {
        val sessionId = createReviewSession(ValidationStatus.UNAVAILABLE)
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when validation is TIMEOUT`() {
        val sessionId = createReviewSession(ValidationStatus.TIMEOUT)
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when validation is PENDING`() {
        val sessionId = createReviewSession(ValidationStatus.PENDING)
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when validation is NOT_STARTED`() {
        val sessionId = createReviewSession(ValidationStatus.NOT_STARTED)
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when validation step state is missing`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.REVIEW,
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
                companyName = "Corp",
                accountId = "id",
                apiKeyPresent = true,
                apiKeyMasked = "****"
            )),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))
        // No VALIDATION step state inserted

        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when details step state is missing`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.REVIEW,
            status = OnboardingSessionStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        ))
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            stepKey = OnboardingStepKey.VALIDATION,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = payloadMapper.toJsonString(ValidationPayload(status = ValidationStatus.VALID)),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))
        // No DETAILS step state inserted

        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }

    @Test
    fun `go-live rejected when session does not exist`() {
        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(UUID.randomUUID())
        }
    }

    @Test
    fun `go-live rejected when session is not in REVIEW step`() {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        // Session at VALIDATION step with VALID status — not yet in REVIEW
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
                companyName = "Corp",
                accountId = "id",
                apiKeyPresent = true,
                apiKeyMasked = "****"
            )),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            stepKey = OnboardingStepKey.VALIDATION,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = payloadMapper.toJsonString(ValidationPayload(status = ValidationStatus.VALID)),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now
        ))

        assertThrows<InvalidWorkflowTransitionException> {
            goLiveService.goLive(sessionId)
        }
    }
}
