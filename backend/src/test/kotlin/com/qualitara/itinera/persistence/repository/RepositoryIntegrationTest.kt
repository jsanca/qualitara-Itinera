package com.qualitara.itinera.persistence.repository

import com.qualitara.itinera.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.persistence.json.VersionedPayload
import com.qualitara.itinera.persistence.model.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
class RepositoryIntegrationTest {

    @Autowired lateinit var sessionRepo: OnboardingSessionRepository
    @Autowired lateinit var stepStateRepo: OnboardingStepStateRepository
    @Autowired lateinit var validationAttemptRepo: ProviderValidationAttemptRepository
    @Autowired lateinit var partnerAccountRepo: PartnerAccountRepository
    @Autowired lateinit var payloadMapper: JsonbPayloadMapper
    @Autowired lateinit var jdbc: NamedParameterJdbcTemplate

    private data class TestPayload(
        override val version: Int = 1,
        val message: String = ""
    ) : VersionedPayload

    @AfterEach
    fun cleanup() {
        // partner_account has no ON DELETE CASCADE, so delete children before parent
        jdbc.update("DELETE FROM partner_account", emptyMap<String, Any>())
        jdbc.update("DELETE FROM provider_validation_attempt", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_step_state", emptyMap<String, Any>())
        jdbc.update("DELETE FROM onboarding_session", emptyMap<String, Any>())
    }

    private fun newSession(step: OnboardingStepKey = OnboardingStepKey.DETAILS): OnboardingSessionRecord {
        val now = OffsetDateTime.now()
        return OnboardingSessionRecord(
            id = UUID.randomUUID(),
            currentStep = step,
            status = OnboardingSessionStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        ).also { sessionRepo.insert(it) }
    }

    @Test
    fun `create and retrieve session`() {
        val session = newSession()
        val found = sessionRepo.findById(session.id)
        assertNotNull(found)
        assertEquals(session.id, found!!.id)
        assertEquals(OnboardingStepKey.DETAILS, found.currentStep)
        assertEquals(OnboardingSessionStatus.DRAFT, found.status)
        assertNull(found.completedAt)
    }

    @Test
    fun `upsert step state replaces existing for same key`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.IN_PROGRESS,
            payloadJson = """{"version":1,"message":"first"}""", payloadVersion = 1,
            createdAt = now, updatedAt = now
        ))
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = """{"version":2,"message":"second"}""", payloadVersion = 2,
            createdAt = now, updatedAt = now
        ))

        val all = stepStateRepo.findAllBySessionId(session.id)
        assertEquals(1, all.size, "Upsert should leave exactly one row per (session_id, step_key)")
        assertEquals(2, all.first().payloadVersion)
        assertEquals(OnboardingStepStatus.COMPLETED, all.first().status)
    }

    @Test
    fun `store and retrieve jsonb payload with version`() {
        val session = newSession()
        val now = OffsetDateTime.now()
        val original = TestPayload(version = 3, message = "roundtrip")

        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.IN_PROGRESS,
            payloadJson = payloadMapper.toJsonString(original), payloadVersion = original.version,
            createdAt = now, updatedAt = now
        ))

        val found = stepStateRepo.findBySessionIdAndStepKey(session.id, OnboardingStepKey.DETAILS)
        assertNotNull(found)
        val retrieved = payloadMapper.fromJsonString(found!!.payloadJson, TestPayload::class.java)
        assertEquals(original.message, retrieved.message)
        assertEquals(original.version, retrieved.version)
        assertEquals(3, found.payloadVersion)
    }

    @Test
    fun `insert and list validation attempts in ascending order`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        // Insert in reverse order to verify ORDER BY
        for (i in 3 downTo 1) {
            validationAttemptRepo.insert(ProviderValidationAttemptRecord(
                id = UUID.randomUUID(), sessionId = session.id, attemptNumber = i,
                accountId = "acme", requestFingerprint = "fp-$i",
                outcome = ProviderValidationOutcome.VALID, startedAt = now
            ))
        }

        val attempts = validationAttemptRepo.findBySessionIdOrderByAttemptNumber(session.id)
        assertEquals(3, attempts.size)
        assertEquals(listOf(1, 2, 3), attempts.map { it.attemptNumber })
        assertEquals(ProviderValidationOutcome.VALID, attempts.first().outcome)
    }

    @Test
    fun `insert and retrieve partner account`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        val account = PartnerAccountRecord(
            id = UUID.randomUUID(), sessionId = session.id,
            companyName = "Acme Inc.", status = PartnerAccountStatus.LIVE,
            wentLiveAt = now, createdAt = now
        )
        partnerAccountRepo.insert(account)

        val bySession = partnerAccountRepo.findBySessionId(session.id)
        assertNotNull(bySession)
        assertEquals("Acme Inc.", bySession!!.companyName)
        assertEquals(PartnerAccountStatus.LIVE, bySession.status)

        val byId = partnerAccountRepo.findById(account.id)
        assertNotNull(byId)
        assertEquals(account.id, byId!!.id)
    }

    @Test
    fun `step state upsert enforces idempotency — two writes produce one row`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        repeat(2) { i ->
            stepStateRepo.upsert(OnboardingStepStateRecord(
                id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.DETAILS,
                status = OnboardingStepStatus.IN_PROGRESS,
                payloadJson = """{"version":${i + 1},"message":"attempt ${i + 1}"}""",
                payloadVersion = i + 1, createdAt = now, updatedAt = now
            ))
        }

        assertEquals(1, stepStateRepo.findAllBySessionId(session.id).size)
    }

    @Test
    fun `validation attempt unique constraint rejects duplicate attempt number`() {
        val session = newSession()
        val now = OffsetDateTime.now()
        val first = ProviderValidationAttemptRecord(
            id = UUID.randomUUID(), sessionId = session.id, attemptNumber = 1,
            accountId = "acme", requestFingerprint = "fp-1",
            outcome = ProviderValidationOutcome.VALID, startedAt = now
        )
        validationAttemptRepo.insert(first)

        assertThrows(DataIntegrityViolationException::class.java) {
            validationAttemptRepo.insert(first.copy(id = UUID.randomUUID()))
        }
    }

    @Test
    fun `partner account session id must be unique`() {
        val session = newSession()
        val now = OffsetDateTime.now()
        partnerAccountRepo.insert(PartnerAccountRecord(
            id = UUID.randomUUID(), sessionId = session.id,
            companyName = "Acme Inc.", status = PartnerAccountStatus.LIVE,
            wentLiveAt = now, createdAt = now
        ))

        assertThrows(DataIntegrityViolationException::class.java) {
            partnerAccountRepo.insert(PartnerAccountRecord(
                id = UUID.randomUUID(), sessionId = session.id,
                companyName = "Duplicate", status = PartnerAccountStatus.LIVE,
                wentLiveAt = now, createdAt = now
            ))
        }
    }

    @Test
    fun `updateCurrentStepAndStatus moves session to next step`() {
        val session = newSession(OnboardingStepKey.DETAILS)
        assertEquals(OnboardingSessionStatus.DRAFT, session.status)

        sessionRepo.updateCurrentStepAndStatus(session.id, OnboardingStepKey.VALIDATION, OnboardingSessionStatus.DRAFT)

        val updated = sessionRepo.findById(session.id)
        assertNotNull(updated)
        assertEquals(OnboardingStepKey.VALIDATION, updated!!.currentStep)
        assertEquals(OnboardingSessionStatus.DRAFT, updated.status)
    }

    @Test
    fun `markCompleted transitions session to LIVE`() {
        val session = newSession(OnboardingStepKey.REVIEW)
        assertNull(session.completedAt)

        val completedAt = OffsetDateTime.now()
        sessionRepo.markCompleted(session.id, completedAt)

        val updated = sessionRepo.findById(session.id)
        assertNotNull(updated)
        assertEquals(OnboardingSessionStatus.LIVE, updated!!.status)
        assertNotNull(updated.completedAt)
    }

    @Test
    fun `findAllBySessionId returns distinct rows for different step keys`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = """{"version":1}""", payloadVersion = 1,
            createdAt = now, updatedAt = now
        ))
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.VALIDATION,
            status = OnboardingStepStatus.IN_PROGRESS,
            payloadJson = """{"version":1}""", payloadVersion = 1,
            createdAt = now, updatedAt = now
        ))
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(), sessionId = session.id, stepKey = OnboardingStepKey.REVIEW,
            status = OnboardingStepStatus.NOT_STARTED,
            payloadJson = """{"version":1}""", payloadVersion = 1,
            createdAt = now, updatedAt = now
        ))

        val all = stepStateRepo.findAllBySessionId(session.id)
        assertEquals(3, all.size)
        val stepKeys = all.map { it.stepKey }.toSet()
        assertEquals(setOf(OnboardingStepKey.DETAILS, OnboardingStepKey.VALIDATION, OnboardingStepKey.REVIEW), stepKeys)
    }

    @Test
    fun `validation attempt with null responsePayloadJson persists and retrieves`() {
        val session = newSession()
        val now = OffsetDateTime.now()

        validationAttemptRepo.insert(ProviderValidationAttemptRecord(
            id = UUID.randomUUID(), sessionId = session.id, attemptNumber = 1,
            accountId = "acme", requestFingerprint = "fp-1",
            outcome = ProviderValidationOutcome.INVALID,
            responsePayloadJson = null,
            errorMessage = "credentials rejected",
            startedAt = now, completedAt = now
        ))

        val found = validationAttemptRepo.findBySessionIdOrderByAttemptNumber(session.id)
        assertEquals(1, found.size)
        assertNull(found.first().responsePayloadJson)
        assertEquals("credentials rejected", found.first().errorMessage)
        assertEquals(ProviderValidationOutcome.INVALID, found.first().outcome)
    }
}
