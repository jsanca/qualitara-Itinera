package com.qualitara.itinera.workflow

import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.workflow.model.WorkflowSessionState

/**
 * Derives the set of actions a partner may take given the current workflow position.
 *
 * This is a pure policy object: it has no side effects, no Spring dependencies, and no
 * I/O. Its only input is [WorkflowSessionState]. It applies domain policy, not numeric
 * calculation — callers should read the result as "what is the partner allowed to do now?"
 *
 * Rules:
 * - [OnboardingStepKey.DETAILS]: partner must submit credentials.
 * - [OnboardingStepKey.VALIDATION]: actions depend on the current [ValidationStatus].
 * - [OnboardingStepKey.REVIEW]: partner may edit credentials or go live.
 * - [OnboardingStepKey.COMPLETE]: no further actions.
 */
object AllowedActionPolicy {

    fun resolve(state: WorkflowSessionState): Set<AllowedAction> = when (state.currentStep) {
        OnboardingStepKey.DETAILS -> setOf(AllowedAction.SUBMIT_DETAILS)

        OnboardingStepKey.VALIDATION -> when (state.validationStatus) {
            ValidationStatus.PENDING ->
                setOf(AllowedAction.EDIT_DETAILS)

            ValidationStatus.UNAVAILABLE, ValidationStatus.TIMEOUT ->
                setOf(AllowedAction.EDIT_DETAILS, AllowedAction.RETRY_VALIDATION)

            ValidationStatus.VALID, ValidationStatus.PARTIAL ->
                setOf(AllowedAction.EDIT_DETAILS, AllowedAction.GO_TO_REVIEW)

            else ->
                // NOT_STARTED, STALE, INVALID — all require a fresh validation attempt
                setOf(AllowedAction.EDIT_DETAILS, AllowedAction.START_VALIDATION)
        }

        OnboardingStepKey.REVIEW -> setOf(AllowedAction.EDIT_DETAILS, AllowedAction.GO_LIVE)

        OnboardingStepKey.COMPLETE -> emptySet()
    }
}
