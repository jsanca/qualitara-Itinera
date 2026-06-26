package com.qualitara.itinera.persistence.model

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a row in the `provider_validation_attempt` table.
 * A full audit log of every Provider validation call made during a session.
 *
 * This record represents a stored row only. It does not own Provider call logic,
 * retry policy, or which outcome is considered "current" for workflow decisions.
 * [com.qualitara.itinera.persistence.repository.ProviderValidationAttemptRepository.nextAttemptNumber]
 * is the only place attempt numbering is derived.
 *
 * @param id unique identifier for this attempt
 * @param sessionId the session this attempt belongs to
 * @param attemptNumber monotonically increasing attempt index within the session
 * @param accountId the account identifier sent to the Provider
 * @param requestFingerprint hash of the request payload for dedup
 * @param outcome the result from the Provider
 * @param responsePayloadJson raw JSONB Provider response, if available
 * @param errorMessage error message if the call failed at the transport layer
 * @param startedAt when the attempt began
 * @param completedAt when the attempt finished, if applicable
 */
data class ProviderValidationAttemptRecord(
    val id: UUID,
    val sessionId: UUID,
    val attemptNumber: Int,
    val accountId: String,
    val requestFingerprint: String,
    val outcome: ProviderValidationOutcome,
    val responsePayloadJson: String? = null,
    val errorMessage: String? = null,
    val startedAt: OffsetDateTime,
    val completedAt: OffsetDateTime? = null
)
