package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.workflow.payload.ProviderItem

/**
 * Result returned by the Provider after credential validation.
 *
 * @param outcome the validation outcome
 * @param items discovered Provider items (present for VALID and PARTIAL)
 * @param warnings warning messages (present for PARTIAL)
 * @param reason human-readable rejection reason (present for INVALID)
 */
data class ProviderValidationResult(
    val outcome: ProviderValidationOutcome,
    val items: List<ProviderItem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val reason: String? = null
)
