package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.workflow.payload.ProviderItem
import org.springframework.stereotype.Component

/**
 * In-process fake implementation of [ProviderValidationPort].
 *
 * Uses deterministic trigger values on `accountId` to produce each outcome.
 * This is for development and testing only — a real implementation would
 * make an HTTP call to the Provider service.
 *
 * Trigger values:
 * - `valid`     → [ProviderValidationOutcome.VALID] with items
 * - `partial`  → [ProviderValidationOutcome.PARTIAL] with items and warnings
 * - `invalid`  → [ProviderValidationOutcome.INVALID] with reason
 * - `unavailable` → [ProviderValidationOutcome.UNAVAILABLE]
 * - `timeout`  → throws [ProviderValidationException] with TIMEOUT
 */
@Component
class FakeProviderValidationClient : ProviderValidationPort {

    override fun validate(request: ProviderValidationRequest): ProviderValidationResult {
        return when (request.accountId) {
            "valid" -> ProviderValidationResult(
                outcome = ProviderValidationOutcome.VALID,
                items = listOf(
                    ProviderItem(externalId = "feed-001", name = "Primary Feed", status = "active"),
                    ProviderItem(externalId = "feed-002", name = "Secondary Feed", status = "active"),
                )
            )
            "partial" -> ProviderValidationResult(
                outcome = ProviderValidationOutcome.PARTIAL,
                items = listOf(
                    ProviderItem(externalId = "feed-001", name = "Primary Feed", status = "active"),
                ),
                warnings = listOf(
                    "Secondary Feed is paused",
                    "Rate limit at 80% capacity",
                )
            )
            "invalid" -> ProviderValidationResult(
                outcome = ProviderValidationOutcome.INVALID,
                reason = "Credentials do not match any account for this organization."
            )
            "unavailable" -> ProviderValidationResult(
                outcome = ProviderValidationOutcome.UNAVAILABLE
            )
            "timeout" -> throw ProviderValidationException(
                "Provider request timed out",
                ProviderValidationOutcome.TIMEOUT
            )
            else -> ProviderValidationResult(
                outcome = ProviderValidationOutcome.INVALID,
                reason = "Unknown account ID: ${request.accountId}"
            )
        }
    }
}
