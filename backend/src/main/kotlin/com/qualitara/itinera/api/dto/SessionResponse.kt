package com.qualitara.itinera.api.dto

import java.util.UUID

/**
 * Full session representation returned by every successful onboarding endpoint.
 *
 * Sensitive fields are excluded: the raw API key and credential fingerprint are never present.
 * All lists are non-null; unavailable details are represented by a null [details] field.
 */
data class SessionResponse(
    val sessionId: UUID,
    val currentStep: String,
    val sessionStatus: String,
    val validationStatus: String,
    val details: DetailsSummary?,
    val validation: ValidationSummary,
    val allowedActions: List<String>
) {
    data class DetailsSummary(
        val companyName: String,
        val accountId: String,
        val apiKeyPresent: Boolean,
        val apiKeyMasked: String?
    )

    data class ValidationSummary(
        val status: String,
        val items: List<ProviderItemDto>,
        val warnings: List<String>,
        val reason: String?
    ) {
        data class ProviderItemDto(
            val externalId: String,
            val name: String,
            val status: String
        )
    }
}
