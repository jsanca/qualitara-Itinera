package com.qualitara.itinera.workflow.payload

import com.qualitara.itinera.internal.persistence.json.VersionedPayload
import java.time.OffsetDateTime

/**
 * Step payload for the REVIEW step.
 *
 * Records whether the partner accepted any warnings and when the review was performed.
 * Both fields are optional since review may be persisted before the partner acts.
 */
data class ReviewPayload(
    override val version: Int = 1,
    val acceptedWarnings: Boolean? = null,
    val reviewedAt: OffsetDateTime? = null
) : VersionedPayload
