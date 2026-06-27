package com.qualitara.itinera.provider

/**
 * Request sent to the Provider for credential validation.
 *
 * @param accountId the account identifier to validate
 * @param apiKey the API key to verify (never logged)
 */
data class ProviderValidationRequest(
    val accountId: String,
    val apiKey: String
)
