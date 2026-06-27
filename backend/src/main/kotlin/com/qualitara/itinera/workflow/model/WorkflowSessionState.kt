package com.qualitara.itinera.workflow.model

import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.workflow.ValidationStatus
import java.util.UUID

/**
 * The complete workflow position of an onboarding session at a point in time.
 *
 * This is a domain object, not a persistence record. It is produced from persistence
 * state by the service layer and consumed by [com.qualitara.itinera.workflow.OnboardingWorkflowService]
 * to compute transitions and allowed actions.
 *
 * Invariants:
 * - [credentialFingerprint] is null until the first successful details submission.
 * - [validationStatus] transitions from STALE back to NOT_STARTED once a new validation
 *   is started, but STALE is never written to the Provider.
 * - This object does not own persistence; callers are responsible for syncing state
 *   back to the repository layer.
 */
data class WorkflowSessionState(
    val sessionId: UUID,
    val currentStep: OnboardingStepKey,
    val validationStatus: ValidationStatus,
    /** SHA-256 fingerprint of accountId + apiKey; null before first submission. */
    val credentialFingerprint: String?
) {
    val isComplete: Boolean get() = currentStep == OnboardingStepKey.COMPLETE
}
