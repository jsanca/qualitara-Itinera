package com.qualitara.itinera.workflow.exception

/**
 * Thrown when a stored JSONB payload carries a version that the application cannot deserialize.
 *
 * This should surface when a payload written by a newer application version is read by
 * an older version that does not know how to handle it.
 */
class UnsupportedPayloadVersionException(
    payloadType: String,
    version: Int
) : RuntimeException("Cannot deserialize $payloadType at version $version — upgrade required")
