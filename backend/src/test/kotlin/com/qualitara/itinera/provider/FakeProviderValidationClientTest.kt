package com.qualitara.itinera.provider

import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeProviderValidationClientTest {

    private val client = FakeProviderValidationClient()

    private fun validRequest() = ProviderValidationRequest(accountId = "any", apiKey = "test-key")

    @Test
    fun `valid accountId returns VALID outcome with items`() {
        val result = client.validate(ProviderValidationRequest(accountId = "valid", apiKey = "key"))

        assertEquals(ProviderValidationOutcome.VALID, result.outcome)
        assertEquals(2, result.items.size)
        assertTrue(result.items.any { it.externalId == "feed-001" })
        assertTrue(result.items.any { it.externalId == "feed-002" })
        assertTrue(result.warnings.isEmpty())
        assertNull(result.reason)
    }

    @Test
    fun `partial accountId returns PARTIAL outcome with items and warnings`() {
        val result = client.validate(ProviderValidationRequest(accountId = "partial", apiKey = "key"))

        assertEquals(ProviderValidationOutcome.PARTIAL, result.outcome)
        assertEquals(1, result.items.size)
        assertEquals("feed-001", result.items.first().externalId)
        assertEquals(2, result.warnings.size)
        assertTrue(result.warnings.any { it.contains("paused") })
        assertNull(result.reason)
    }

    @Test
    fun `invalid accountId returns INVALID outcome with reason`() {
        val result = client.validate(ProviderValidationRequest(accountId = "invalid", apiKey = "key"))

        assertEquals(ProviderValidationOutcome.INVALID, result.outcome)
        assertTrue(result.items.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.reason!!.contains("Credentials"))
    }

    @Test
    fun `unavailable accountId returns UNAVAILABLE outcome with no items or reason`() {
        val result = client.validate(ProviderValidationRequest(accountId = "unavailable", apiKey = "key"))

        assertEquals(ProviderValidationOutcome.UNAVAILABLE, result.outcome)
        assertTrue(result.items.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertNull(result.reason)
    }

    @Test
    fun `timeout accountId throws ProviderValidationException with TIMEOUT outcome`() {
        val error = assertThrows<ProviderValidationException> {
            client.validate(ProviderValidationRequest(accountId = "timeout", apiKey = "key"))
        }

        assertEquals(ProviderValidationOutcome.TIMEOUT, error.outcome)
        assertTrue(error.message!!.contains("timed out"))
    }

    @Test
    fun `unknown accountId returns INVALID with unknown account reason`() {
        val result = client.validate(ProviderValidationRequest(accountId = "unknown-account", apiKey = "key"))

        assertEquals(ProviderValidationOutcome.INVALID, result.outcome)
        assertTrue(result.reason!!.contains("Unknown account ID"))
    }
}
