package com.qualitara.itinera.provider

/**
 * Port for Provider credential validation.
 *
 * Implementations may be real HTTP clients, fake in-process clients, or test stubs.
 * This interface does not own: retry policy, timeout configuration, or result caching.
 */
interface ProviderValidationPort {
    /**
     * Validates the given Provider credentials.
     *
     * @param request the validation request containing accountId and apiKey
     * @return a result describing the validation outcome
     * @throws ProviderValidationException for transport-level failures (timeout, unreachable)
     */
    fun validate(request: ProviderValidationRequest): ProviderValidationResult
}
