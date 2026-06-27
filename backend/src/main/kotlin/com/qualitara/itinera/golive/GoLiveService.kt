package com.qualitara.itinera.golive

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.PartnerAccountRecord
import com.qualitara.itinera.internal.persistence.model.PartnerAccountStatus
import com.qualitara.itinera.internal.persistence.repository.OnboardingSessionRepository
import com.qualitara.itinera.internal.persistence.repository.OnboardingStepStateRepository
import com.qualitara.itinera.internal.persistence.repository.PartnerAccountRepository
import com.qualitara.itinera.workflow.OnboardingWorkflowService
import com.qualitara.itinera.workflow.ValidationStatus
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.model.WorkflowSessionState
import com.qualitara.itinera.workflow.payload.DetailsPayload
import com.qualitara.itinera.workflow.payload.ValidationPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Orchestrates the transactional go-live transition for an onboarding session.
 *
 * Responsibilities:
 * - Load and validate session and step state from persistence.
 * - Enforce go-live eligibility through [OnboardingWorkflowService].
 * - Create one [com.qualitara.itinera.internal.persistence.model.PartnerAccountRecord] per session.
 * - Mark the session COMPLETE and LIVE atomically.
 * - Return the completed [WorkflowSessionState].
 *
 * Does not own:
 * - Workflow transition rules (owned by [OnboardingWorkflowService]).
 * - Provider validation (owned by [com.qualitara.itinera.provider.ProviderValidationService]).
 * - REST response shaping (owned by the controller layer).
 *
 * ## Idempotency
 *
 * If the session is already COMPLETE + LIVE with an existing partner account,
 * [goLive] returns the completed state without creating a duplicate account or
 * modifying any existing records. The unique constraint on `partner_account.session_id`
 * is a structural safeguard but the service makes the idempotency check explicit.
 */
@Service
class GoLiveService(
    private val sessionRepo: OnboardingSessionRepository,
    private val stepStateRepo: OnboardingStepStateRepository,
    private val partnerAccountRepo: PartnerAccountRepository,
    private val workflowService: OnboardingWorkflowService,
    private val payloadMapper: JsonbPayloadMapper
) {

    /**
     * Executes the go-live transition for the given session.
     *
     * @param sessionId the session to go live
     * @return the updated [WorkflowSessionState] with [OnboardingStepKey.COMPLETE]
     * @throws InvalidWorkflowTransitionException if the session is missing, step states
     *   are absent, or the workflow state does not permit go-live.
     */
    @Transactional
    fun goLive(sessionId: UUID): WorkflowSessionState {
        log.debug { "Go-live requested for session=$sessionId" }

        val session = sessionRepo.findById(sessionId)
            ?: throw InvalidWorkflowTransitionException("Session not found: $sessionId")

        // Idempotency: session already completed and partner account exists
        if (session.currentStep == OnboardingStepKey.COMPLETE && session.status == OnboardingSessionStatus.LIVE) {
            val existingAccount = partnerAccountRepo.findBySessionId(sessionId)
            if (existingAccount != null) {
                log.debug { "Session $sessionId already live — idempotent return" }
                val validationPayload = resolveValidationPayload(sessionId)
                return WorkflowSessionState(
                    sessionId = sessionId,
                    currentStep = OnboardingStepKey.COMPLETE,
                    validationStatus = validationPayload.status,
                    credentialFingerprint = null
                )
            }
        }

        val validationRecord = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?: throw InvalidWorkflowTransitionException("No validation step state found for session $sessionId")
        val validationPayload = payloadMapper.fromJsonString(validationRecord.payloadJson, ValidationPayload::class.java)

        val detailsRecord = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.DETAILS)
            ?: throw InvalidWorkflowTransitionException("No details step state found for session $sessionId")
        val details = payloadMapper.fromJsonString(detailsRecord.payloadJson, DetailsPayload::class.java)

        val state = WorkflowSessionState(
            sessionId = sessionId,
            currentStep = session.currentStep,
            validationStatus = validationPayload.status,
            credentialFingerprint = null
        )

        val finalState = try {
            workflowService.applyGoLive(state)
        } catch (e: InvalidWorkflowTransitionException) {
            log.warn { "Go-live rejected for session=$sessionId: ${e.message}" }
            throw e
        }

        val now = OffsetDateTime.now()
        partnerAccountRepo.insert(PartnerAccountRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            companyName = details.companyName,
            status = PartnerAccountStatus.LIVE,
            wentLiveAt = now,
            createdAt = now
        ))

        sessionRepo.markLiveAndComplete(sessionId, now)

        log.debug { "Session $sessionId went live (companyName=${details.companyName})" }
        return finalState
    }

    private fun resolveValidationPayload(sessionId: UUID): ValidationPayload {
        val record = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?: return ValidationPayload(status = ValidationStatus.NOT_STARTED)
        return payloadMapper.fromJsonString(record.payloadJson, ValidationPayload::class.java)
    }
}
