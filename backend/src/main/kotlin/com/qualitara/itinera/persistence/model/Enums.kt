package com.qualitara.itinera.persistence.model

/**
 * Keys identifying each step in the onboarding workflow.
 * These are the stable identifiers used in [OnboardingStepStateRecord] and workflow transitions.
 */
enum class OnboardingStepKey {
    DETAILS, VALIDATION, REVIEW, COMPLETE
}

/**
 * Lifecycle status of an onboarding session.
 * DRAFT = in progress; LIVE = completed and partner account active.
 */
enum class OnboardingSessionStatus {
    DRAFT, LIVE
}

/**
 * Completion status of an individual step within a session.
 * BLOCKED indicates the step cannot proceed due to a prior step failure.
 */
enum class OnboardingStepStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED
}

/**
 * Outcome of a Provider validation call.
 * These values are persisted and used to drive workflow transitions.
 */
enum class ProviderValidationOutcome {
    VALID, PARTIAL, INVALID, UNAVAILABLE, TIMEOUT
}

/**
 * Status of a partner account. Currently only LIVE is used.
 */
enum class PartnerAccountStatus {
    LIVE
}
