package com.qualitara.itinera.internal.persistence.model

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a row in the `onboarding_session` table.
 * Encapsulates the current workflow step and session lifecycle status.
 *
 * This record is a persistence representation only. It holds no business logic
 * and is not responsible for workflow transitions, validation, or state machine rules.
 *
 * @param id unique session identifier
 * @param currentStep the step key where the session currently sits
 * @param status DRAFT while onboarding is in progress; LIVE when complete
 * @param createdAt when the session was created
 * @param updatedAt when the session was last modified
 * @param completedAt when the session reached LIVE status, if applicable
 */
data class OnboardingSessionRecord(
    val id: UUID,
    val currentStep: OnboardingStepKey,
    val status: OnboardingSessionStatus,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val completedAt: OffsetDateTime? = null
)
