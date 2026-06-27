package com.qualitara.itinera.workflow

/**
 * Workflow-level understanding of where Provider validation stands for a session.
 *
 * This is distinct from [com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome],
 * which is the raw outcome returned by the Provider. [ValidationStatus] adds workflow-specific
 * states ([NOT_STARTED], [STALE]) that have no equivalent in the Provider response.
 */
enum class ValidationStatus {

    /** No validation has been attempted yet. */
    NOT_STARTED,

    /** Validation is currently in progress (async or deferred). */
    PENDING,

    /** Provider confirmed credentials are fully valid. */
    VALID,

    /** Provider returned a valid result with warnings. */
    PARTIAL,

    /** Provider rejected the credentials. */
    INVALID,

    /** Provider was temporarily unavailable; retry is safe. */
    UNAVAILABLE,

    /** Provider call timed out; retry is safe. */
    TIMEOUT,

    /**
     * Credentials changed after a previous validation result.
     * The prior result can no longer be trusted and validation must be restarted.
     *
     * This state exists to make BR-001 explicit in the domain model rather than
     * relying on callers to check fingerprints before trusting validation state.
     */
    STALE
}
