package com.qualitara.itinera.workflow

import java.security.MessageDigest

/**
 * Computes a credential fingerprint used to detect when a partner changes their Provider credentials.
 *
 * The fingerprint is a SHA-256 hex digest of `accountId:apiKey`. It is the only credential-derived
 * value stored in workflow state; the raw API key is never persisted or logged.
 *
 * Used by [OnboardingWorkflowService] to enforce BR-001: changing credentials after a successful
 * validation marks the previous result as [ValidationStatus.STALE].
 */
object CredentialFingerprint {

    fun compute(accountId: String, apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$accountId:$apiKey".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
