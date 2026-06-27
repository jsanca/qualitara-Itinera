package com.qualitara.itinera.workflow

import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.model.WorkflowSessionState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Domain service for the partner onboarding workflow.
 *
 * This service owns all workflow state transitions. It does not own:
 * - HTTP handling (that belongs to the controller layer)
 * - persistence SQL (that belongs to the repository layer)
 * - Provider calls (those go through ProviderValidationPort)
 * - frontend rendering state
 *
 * Callers are responsible for loading [WorkflowSessionState] from repositories,
 * passing it here for transition, and then persisting the returned state.
 *
 * All methods are pure domain operations that return a new state object.
 *
 * ## Validation lifecycle
 *
 * Validation must follow an explicit two-step protocol:
 * 1. Call [startValidation] to acquire a [ValidationStatus.PENDING] state.
 * 2. Call [applyValidationOutcome] with the Provider result.
 *
 * [applyValidationOutcome] rejects any state that is not PENDING. This ensures
 * every outcome is preceded by a recorded start and prevents silent state skips.
 */
@Service
class OnboardingWorkflowService {

    /**
     * Produces the initial workflow state for a newly created session.
     */
    fun newSession(sessionId: UUID): WorkflowSessionState {
        log.debug { "Initializing new session state: sessionId=$sessionId" }
        return WorkflowSessionState(
            sessionId = sessionId,
            currentStep = OnboardingStepKey.DETAILS,
            validationStatus = ValidationStatus.NOT_STARTED,
            credentialFingerprint = null
        )
    }

    /**
     * Applies a details submission to the current workflow state.
     *
     * Advancing rules:
     * - Rejects if the session is [OnboardingStepKey.COMPLETE] (BR-002: completed sessions cannot be reopened).
     * - Always advances from [OnboardingStepKey.DETAILS] to [OnboardingStepKey.VALIDATION].
     * - If the session is already past DETAILS and credentials have not changed, the step is preserved.
     * - BR-001: if credentials changed (fingerprint differs), previous validation is invalidated
     *   by setting status to [ValidationStatus.STALE] and returning to [OnboardingStepKey.VALIDATION].
     *
     * @throws InvalidWorkflowTransitionException if the session is COMPLETE.
     */
    fun applyDetailsSubmission(state: WorkflowSessionState, newFingerprint: String): WorkflowSessionState {
        if (state.currentStep == OnboardingStepKey.COMPLETE) {
            throw InvalidWorkflowTransitionException(
                "Cannot submit details on a completed session (session=${state.sessionId})"
            )
        }

        val credentialChanged = state.credentialFingerprint != null && state.credentialFingerprint != newFingerprint

        return if (credentialChanged) {
            log.debug { "BR-001: credential fingerprint changed for session=${state.sessionId}, invalidating prior validation" }
            state.copy(
                currentStep = OnboardingStepKey.VALIDATION,
                validationStatus = ValidationStatus.STALE,
                credentialFingerprint = newFingerprint
            )
        } else {
            val nextStep = if (state.currentStep == OnboardingStepKey.DETAILS) OnboardingStepKey.VALIDATION else state.currentStep
            log.debug { "Details submitted for session=${state.sessionId}, step=${nextStep}" }
            state.copy(
                currentStep = nextStep,
                credentialFingerprint = newFingerprint
            )
        }
    }

    /**
     * Marks the session as ready for a Provider validation call by setting status to [ValidationStatus.PENDING].
     *
     * Allowed from [OnboardingStepKey.VALIDATION] when current status is one of:
     * [ValidationStatus.NOT_STARTED], [ValidationStatus.STALE], [ValidationStatus.INVALID],
     * [ValidationStatus.UNAVAILABLE], [ValidationStatus.TIMEOUT].
     *
     * Rejected when:
     * - The session is not in [OnboardingStepKey.VALIDATION].
     * - Status is already [ValidationStatus.PENDING] (re-entrant start is not allowed).
     * - Status is [ValidationStatus.VALID] or [ValidationStatus.PARTIAL] (already succeeded; use GO_TO_REVIEW).
     *
     * @throws InvalidWorkflowTransitionException if preconditions are not met.
     */
    fun startValidation(state: WorkflowSessionState): WorkflowSessionState {
        if (state.currentStep != OnboardingStepKey.VALIDATION) {
            throw InvalidWorkflowTransitionException(
                "startValidation requires VALIDATION step, current step is ${state.currentStep} (session=${state.sessionId})"
            )
        }

        val retryableStatuses = setOf(
            ValidationStatus.NOT_STARTED,
            ValidationStatus.STALE,
            ValidationStatus.INVALID,
            ValidationStatus.UNAVAILABLE,
            ValidationStatus.TIMEOUT
        )

        if (state.validationStatus !in retryableStatuses) {
            throw InvalidWorkflowTransitionException(
                "startValidation rejected: status ${state.validationStatus} is not retryable (session=${state.sessionId})"
            )
        }

        log.debug { "Validation started for session=${state.sessionId}, was ${state.validationStatus}" }
        return state.copy(validationStatus = ValidationStatus.PENDING)
    }

    /**
     * Applies a Provider validation outcome to the current workflow state.
     *
     * This method requires [ValidationStatus.PENDING] as a precondition. Callers must
     * call [startValidation] before calling this method.
     *
     * Outcome mapping:
     * - [ProviderValidationOutcome.VALID] / [ProviderValidationOutcome.PARTIAL] → advance to [OnboardingStepKey.REVIEW]
     * - [ProviderValidationOutcome.INVALID] → return to [OnboardingStepKey.DETAILS]
     * - [ProviderValidationOutcome.UNAVAILABLE] / [ProviderValidationOutcome.TIMEOUT] → stay in [OnboardingStepKey.VALIDATION] (retry safe)
     *
     * @throws InvalidWorkflowTransitionException if the session is not in VALIDATION step or status is not PENDING.
     */
    fun applyValidationOutcome(
        state: WorkflowSessionState,
        outcome: ProviderValidationOutcome
    ): WorkflowSessionState {
        if (state.currentStep != OnboardingStepKey.VALIDATION) {
            throw InvalidWorkflowTransitionException(
                "Cannot apply validation outcome in step ${state.currentStep} (session=${state.sessionId})"
            )
        }
        if (state.validationStatus != ValidationStatus.PENDING) {
            throw InvalidWorkflowTransitionException(
                "applyValidationOutcome requires PENDING status, current status is ${state.validationStatus} (session=${state.sessionId})"
            )
        }

        return when (outcome) {
            ProviderValidationOutcome.VALID -> {
                log.debug { "Validation VALID: session=${state.sessionId} advancing to REVIEW" }
                state.copy(currentStep = OnboardingStepKey.REVIEW, validationStatus = ValidationStatus.VALID)
            }
            ProviderValidationOutcome.PARTIAL -> {
                log.debug { "Validation PARTIAL: session=${state.sessionId} advancing to REVIEW" }
                state.copy(currentStep = OnboardingStepKey.REVIEW, validationStatus = ValidationStatus.PARTIAL)
            }
            ProviderValidationOutcome.INVALID -> {
                log.warn { "Validation INVALID: session=${state.sessionId} returning to DETAILS" }
                state.copy(currentStep = OnboardingStepKey.DETAILS, validationStatus = ValidationStatus.INVALID)
            }
            ProviderValidationOutcome.UNAVAILABLE -> {
                log.warn { "Validation UNAVAILABLE: session=${state.sessionId} remains retryable" }
                state.copy(validationStatus = ValidationStatus.UNAVAILABLE)
            }
            ProviderValidationOutcome.TIMEOUT -> {
                log.warn { "Validation TIMEOUT: session=${state.sessionId} remains retryable" }
                state.copy(validationStatus = ValidationStatus.TIMEOUT)
            }
        }
    }

    /**
     * Applies a go-live transition.
     *
     * Go-live is only permitted when the session is in [OnboardingStepKey.REVIEW] and the
     * validation status is [ValidationStatus.VALID] or [ValidationStatus.PARTIAL].
     *
     * The validation status is preserved on completion for historical context.
     * This method returns the updated state. The caller is responsible for executing the
     * go-live transaction (creating the partner account) in the repository layer.
     *
     * @throws InvalidWorkflowTransitionException if preconditions are not met.
     */
    fun applyGoLive(state: WorkflowSessionState): WorkflowSessionState {
        if (state.currentStep != OnboardingStepKey.REVIEW) {
            throw InvalidWorkflowTransitionException(
                "Go-live requires REVIEW step, current step is ${state.currentStep} (session=${state.sessionId})"
            )
        }
        if (state.validationStatus != ValidationStatus.VALID && state.validationStatus != ValidationStatus.PARTIAL) {
            throw InvalidWorkflowTransitionException(
                "Go-live requires VALID or PARTIAL validation, current status is ${state.validationStatus} (session=${state.sessionId})"
            )
        }

        log.debug { "Go-live approved for session=${state.sessionId}" }
        return state.copy(currentStep = OnboardingStepKey.COMPLETE)
        // validationStatus is intentionally preserved to record the final outcome
    }

    /**
     * Returns the set of actions currently allowed for the given workflow state.
     *
     * Delegates to [AllowedActionPolicy]. The result is intended to be included in
     * API responses so the frontend knows which operations are available.
     */
    fun allowedActions(state: WorkflowSessionState): Set<AllowedAction> = AllowedActionPolicy.resolve(state)
}
