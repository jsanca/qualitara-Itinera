package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStateRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStatus
import com.qualitara.itinera.internal.persistence.model.ProviderValidationAttemptRecord
import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.internal.persistence.repository.OnboardingSessionRepository
import com.qualitara.itinera.internal.persistence.repository.OnboardingStepStateRepository
import com.qualitara.itinera.internal.persistence.repository.ProviderValidationAttemptRepository
import com.qualitara.itinera.workflow.CredentialFingerprint
import com.qualitara.itinera.workflow.OnboardingWorkflowService
import com.qualitara.itinera.workflow.ValidationStatus
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.model.WorkflowSessionState
import com.qualitara.itinera.workflow.payload.DetailsPayload
import com.qualitara.itinera.workflow.payload.ProviderItem
import com.qualitara.itinera.workflow.payload.ValidationPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Orchestrates Provider credential validation for an onboarding session.
 *
 * Responsibilities:
 * - Load session and details state from persistence.
 * - Execute the validation lifecycle: PENDING → Provider call → outcome.
 * - Record every attempt in the audit log.
 * - Persist the latest validation payload into step state.
 * - Advance the session step when the outcome changes it (VALID/PARTIAL → REVIEW; INVALID → DETAILS).
 *
 * Does not own:
 * - Workflow transition rules (owned by [OnboardingWorkflowService]).
 * - Provider communication details (owned by [ProviderValidationPort]).
 * - HTTP request/response mapping (owned by the REST controller).
 *
 * ## Transaction note
 *
 * The entire [validate] method runs in one Spring-managed transaction. For the synchronous
 * in-process [FakeProviderValidationClient] this is acceptable. A real HTTP-backed Provider
 * would require splitting the transaction: persist PENDING before the HTTP call, then
 * record the result in a separate transaction after the call completes.
 */
@Service
class ProviderValidationService(
    private val sessionRepo: OnboardingSessionRepository,
    private val stepStateRepo: OnboardingStepStateRepository,
    private val attemptRepo: ProviderValidationAttemptRepository,
    private val providerPort: ProviderValidationPort,
    private val workflowService: OnboardingWorkflowService,
    private val payloadMapper: JsonbPayloadMapper
) {

    /**
     * Runs a Provider validation attempt for the given session.
     *
     * The [apiKey] is used to call the Provider but is never persisted. Only a SHA-256
     * fingerprint of `accountId:apiKey` is stored in the attempt record.
     *
     * @param sessionId the session to validate
     * @param apiKey the raw API key supplied by the partner (not stored)
     * @return the updated [WorkflowSessionState] after the outcome is applied
     * @throws InvalidWorkflowTransitionException if session is missing, details are absent,
     *   or the current workflow state does not allow validation.
     */
    @Transactional
    fun validate(sessionId: UUID, apiKey: String): WorkflowSessionState {
        val session = sessionRepo.findById(sessionId)
            ?: throw InvalidWorkflowTransitionException("Session not found: $sessionId")

        val detailsRecord = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.DETAILS)
            ?: throw InvalidWorkflowTransitionException("No details submitted for session $sessionId")
        val details = payloadMapper.fromJsonString(detailsRecord.payloadJson, DetailsPayload::class.java)

        val state = WorkflowSessionState(
            sessionId = sessionId,
            currentStep = session.currentStep,
            validationStatus = resolveValidationStatus(sessionId),
            credentialFingerprint = details.credentialFingerprint
        )

        // Transition to PENDING and persist immediately so resume is possible
        val pendingState = workflowService.startValidation(state)
        persistValidationPayload(sessionId, ValidationPayload(status = ValidationStatus.PENDING), OnboardingStepStatus.IN_PROGRESS)

        // Call Provider — exceptions from transport-level failures are captured, not propagated
        val requestFingerprint = CredentialFingerprint.compute(details.accountId, apiKey)
        val attemptNumber = attemptRepo.nextAttemptNumber(sessionId)
        val startedAt = OffsetDateTime.now()

        val callResult = callProvider(ProviderValidationRequest(details.accountId, apiKey))
        val completedAt = OffsetDateTime.now()

        log.debug { "Validation outcome=${callResult.outcome} session=$sessionId attempt=$attemptNumber" }

        // Audit record — every call produces a row regardless of outcome
        attemptRepo.insert(ProviderValidationAttemptRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            attemptNumber = attemptNumber,
            accountId = details.accountId,
            requestFingerprint = requestFingerprint,
            outcome = callResult.outcome,
            responsePayloadJson = callResult.responseJson,
            errorMessage = callResult.errorMessage,
            startedAt = startedAt,
            completedAt = completedAt
        ))

        // Apply outcome through domain workflow
        val finalState = workflowService.applyValidationOutcome(pendingState, callResult.outcome)

        // Persist final validation state, replacing the PENDING placeholder
        val (payload, stepStatus) = buildValidationPayloadAndStatus(callResult)
        persistValidationPayload(sessionId, payload, stepStatus)

        // Advance the session step if the outcome changed it
        if (finalState.currentStep != session.currentStep) {
            log.debug { "Session $sessionId step: ${session.currentStep} → ${finalState.currentStep}" }
            sessionRepo.updateCurrentStepAndStatus(sessionId, finalState.currentStep, OnboardingSessionStatus.DRAFT)
        }

        return finalState
    }

    private fun resolveValidationStatus(sessionId: UUID): ValidationStatus {
        val record = stepStateRepo.findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?: return ValidationStatus.NOT_STARTED
        return payloadMapper.fromJsonString(record.payloadJson, ValidationPayload::class.java).status
    }

    private fun callProvider(request: ProviderValidationRequest): ProviderCallResult {
        return try {
            val result = providerPort.validate(request)
            ProviderCallResult(
                outcome = result.outcome,
                items = result.items,
                warnings = result.warnings,
                reason = result.reason,
                responseJson = payloadMapper.toJsonString(result)
            )
        } catch (e: ProviderValidationException) {
            log.warn { "Provider transport failure: ${e.message} → outcome=${e.outcome}" }
            ProviderCallResult(outcome = e.outcome, errorMessage = e.message)
        }
    }

    private fun buildValidationPayloadAndStatus(result: ProviderCallResult): Pair<ValidationPayload, OnboardingStepStatus> =
        when (result.outcome) {
            ProviderValidationOutcome.VALID -> Pair(
                ValidationPayload(status = ValidationStatus.VALID, items = result.items),
                OnboardingStepStatus.COMPLETED
            )
            ProviderValidationOutcome.PARTIAL -> Pair(
                ValidationPayload(status = ValidationStatus.PARTIAL, items = result.items, warnings = result.warnings),
                OnboardingStepStatus.COMPLETED
            )
            ProviderValidationOutcome.INVALID -> Pair(
                ValidationPayload(status = ValidationStatus.INVALID, reason = result.reason),
                OnboardingStepStatus.BLOCKED
            )
            ProviderValidationOutcome.UNAVAILABLE -> Pair(
                ValidationPayload(status = ValidationStatus.UNAVAILABLE),
                OnboardingStepStatus.IN_PROGRESS
            )
            ProviderValidationOutcome.TIMEOUT -> Pair(
                ValidationPayload(status = ValidationStatus.TIMEOUT),
                OnboardingStepStatus.IN_PROGRESS
            )
        }

    private fun persistValidationPayload(
        sessionId: UUID,
        payload: ValidationPayload,
        stepStatus: OnboardingStepStatus
    ) {
        val now = OffsetDateTime.now()
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), // preserved on upsert conflict; only used for initial insert
            sessionId = sessionId,
            stepKey = OnboardingStepKey.VALIDATION,
            status = stepStatus,
            payloadJson = payloadMapper.toJsonString(payload),
            payloadVersion = payload.version,
            createdAt = now, // preserved on upsert conflict; only used for initial insert
            updatedAt = now,
            completedAt = if (stepStatus == OnboardingStepStatus.COMPLETED) now else null
        ))
    }

    private data class ProviderCallResult(
        val outcome: ProviderValidationOutcome,
        val items: List<ProviderItem> = emptyList(),
        val warnings: List<String> = emptyList(),
        val reason: String? = null,
        val responseJson: String? = null,
        val errorMessage: String? = null
    )
}
