package com.qualitara.itinera.workflow

/**
 * Actions the frontend is permitted to invoke on a session, based on current workflow state.
 *
 * Allowed actions are computed by [AllowedActionCalculator] from the current step and
 * validation status. The frontend must not invent transitions — it renders and invokes
 * only the actions returned by the backend.
 */
enum class AllowedAction {
    /** Submit or re-submit company and Provider credentials. */
    SUBMIT_DETAILS,

    /** Navigate back to the details step to correct credentials. */
    EDIT_DETAILS,

    /** Trigger Provider validation for the first time. */
    START_VALIDATION,

    /** Retry Provider validation after a transient failure. */
    RETRY_VALIDATION,

    /** Navigate to the review step after a successful or partial validation. */
    GO_TO_REVIEW,

    /** Finalize onboarding and create the live partner account. */
    GO_LIVE
}
