package com.qualitara.itinera.persistence.model

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a row in the `onboarding_step_state` table.
 * Stores the status and JSONB payload for one step of an onboarding session.
 *
 * This record represents a stored row only. It does not own payload schema evolution,
 * business validation of payload contents, or step-level transition rules.
 * Payload shape is the responsibility of the caller; [com.qualitara.itinera.persistence.json.VersionedPayload]
 * provides a structural versioning marker only.
 *
 * @param id unique identifier for this step state record
 * @param sessionId the session this state belongs to
 * @param stepKey which step this state is for
 * @param status step-level completion status
 * @param payloadJson raw JSONB payload; caller is responsible for serialization/deserialization
 * @param payloadVersion structural version of the payload; used to detect shape changes, not schema migration
 * @param createdAt when this step state was first created
 * @param updatedAt when this step state was last modified
 * @param completedAt when the step reached COMPLETED status, if applicable
 */
data class OnboardingStepStateRecord(
    val id: UUID,
    val sessionId: UUID,
    val stepKey: OnboardingStepKey,
    val status: OnboardingStepStatus,
    val payloadJson: String,
    val payloadVersion: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val completedAt: OffsetDateTime? = null
)
