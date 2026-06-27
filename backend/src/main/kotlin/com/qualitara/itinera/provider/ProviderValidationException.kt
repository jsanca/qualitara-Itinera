package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome

/**
 * Thrown when a Provider validation call fails at the transport layer.
 *
 * This exception is part of the [ProviderValidationPort] contract.
 * All implementations — including [FakeProviderValidationClient] and any future HTTP client —
 * must use this exception for transport-level failures such as timeouts or unreachable endpoints.
 *
 * The [outcome] field indicates the presumed result when the transport error occurred
 * (e.g., [ProviderValidationOutcome.TIMEOUT] when the call did not complete in time).
 */
class ProviderValidationException(
    message: String,
    val outcome: ProviderValidationOutcome
) : RuntimeException(message)
