package com.qualitara.itinera.workflow.payload

import com.qualitara.itinera.internal.persistence.json.VersionedPayload

/**
 * Step payload for the DETAILS step.
 *
 * The raw API key is never stored here. Only [apiKeyPresent] and [apiKeyMasked] are persisted
 * so the frontend can confirm that credentials are on file without re-displaying the key.
 *
 * The credential fingerprint (SHA-256 of accountId + apiKey) is stored separately in
 * [com.qualitara.itinera.workflow.model.WorkflowSessionState] and is used solely to
 * detect credential changes (BR-001). It is not exposed through the API.
 */
data class DetailsPayload(
    override val version: Int = 1,
    val companyName: String,
    val accountId: String,
    val apiKeyPresent: Boolean,
    val apiKeyMasked: String?
) : VersionedPayload
