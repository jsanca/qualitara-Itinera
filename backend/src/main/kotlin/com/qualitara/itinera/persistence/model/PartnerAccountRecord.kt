package com.qualitara.itinera.persistence.model

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Represents a row in the `partner_account` table.
 * The persisted result of a successful go-live transition.
 *
 * This record represents a stored row only. It does not own go-live transaction logic,
 * idempotency guarantees, or account activation policy. Unique constraint on
 * `session_id` is the database-level anchor for idempotent account creation.
 *
 * @param id unique account identifier
 * @param sessionId the onboarding session that produced this account
 * @param companyName display name recorded at go-live time
 * @param status account status (currently always LIVE)
 * @param wentLiveAt when the account was activated
 * @param createdAt when this record was created
 */
data class PartnerAccountRecord(
    val id: UUID,
    val sessionId: UUID,
    val companyName: String,
    val status: PartnerAccountStatus,
    val wentLiveAt: OffsetDateTime,
    val createdAt: OffsetDateTime
)
