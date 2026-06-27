package com.qualitara.itinera.workflow.payload

import com.qualitara.itinera.internal.persistence.json.VersionedPayload

/**
 * Step payload for the DETAILS step.
 *
 * The raw API key is never stored here. Only [apiKeyPresent] and [apiKeyMasked] are persisted
 * so the frontend can confirm that credentials are on file without re-displaying the key.
 *
 * [credentialFingerprint] is the SHA-256 hex digest of `accountId:apiKey`. It is stored here
 * so that [com.qualitara.itinera.workflow.model.WorkflowSessionState] can be reconstructed
 * with the correct fingerprint after a session reload or server restart. Without it, BR-001
 * (credential-change invalidation) cannot be enforced across resumptions.
 *
 * The fingerprint must not be included in frontend-facing response DTOs.
 */
data class DetailsPayload(
    override val version: Int = 1,
    val companyName: String,
    val accountId: String,
    val apiKeyPresent: Boolean,
    val apiKeyMasked: String?,
    val credentialFingerprint: String? = null
) : VersionedPayload
