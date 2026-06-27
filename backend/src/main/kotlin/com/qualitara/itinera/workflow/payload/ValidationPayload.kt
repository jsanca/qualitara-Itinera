package com.qualitara.itinera.workflow.payload

import com.qualitara.itinera.internal.persistence.json.VersionedPayload
import com.qualitara.itinera.workflow.ValidationStatus

/**
 * Step payload for the VALIDATION step.
 *
 * Captures the latest validation outcome including discovered Provider items and any warnings.
 * Transient statuses ([ValidationStatus.UNAVAILABLE], [ValidationStatus.TIMEOUT],
 * [ValidationStatus.STALE]) are stored here so the frontend can show the reason for
 * the current validation state without re-running validation.
 */
data class ValidationPayload(
    override val version: Int = 1,
    val status: ValidationStatus,
    val items: List<ProviderItem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val reason: String? = null
) : VersionedPayload
