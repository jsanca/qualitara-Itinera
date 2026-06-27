package com.qualitara.itinera.workflow

import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.model.WorkflowSessionState
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingWorkflowServiceTest {

    private val service = OnboardingWorkflowService()

    private val sessionId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val fingerprint = "abc123"
    private val newFingerprint = "def456"

    private fun stateAt(
        step: OnboardingStepKey,
        validationStatus: ValidationStatus = ValidationStatus.NOT_STARTED,
        credentialFingerprint: String? = fingerprint
    ) = WorkflowSessionState(
        sessionId = sessionId,
        currentStep = step,
        validationStatus = validationStatus,
        credentialFingerprint = credentialFingerprint
    )

    private fun pendingValidation(): WorkflowSessionState =
        service.startValidation(stateAt(OnboardingStepKey.VALIDATION))

    // --- Original 12 scenarios (some updated for explicit PENDING lifecycle) ---

    // 1. New session starts at DETAILS
    @Test
    fun `newSession starts at DETAILS with no fingerprint`() {
        val state = service.newSession(sessionId)
        assertEquals(OnboardingStepKey.DETAILS, state.currentStep)
        assertEquals(ValidationStatus.NOT_STARTED, state.validationStatus)
        assertNull(state.credentialFingerprint)
    }

    // 2. Submitting details moves to VALIDATION
    @Test
    fun `applyDetailsSubmission from DETAILS advances to VALIDATION`() {
        val initial = stateAt(OnboardingStepKey.DETAILS, credentialFingerprint = null)
        val next = service.applyDetailsSubmission(initial, fingerprint)
        assertEquals(OnboardingStepKey.VALIDATION, next.currentStep)
        assertEquals(fingerprint, next.credentialFingerprint)
    }

    // 3. Details submitted twice with same fingerprint remains idempotent
    @Test
    fun `applyDetailsSubmission with same fingerprint is idempotent`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.NOT_STARTED, fingerprint)
        val next = service.applyDetailsSubmission(state, fingerprint)
        assertEquals(OnboardingStepKey.VALIDATION, next.currentStep)
        assertEquals(ValidationStatus.NOT_STARTED, next.validationStatus)
        assertEquals(fingerprint, next.credentialFingerprint)
    }

    // 4. Editing credentials after valid validation marks validation STALE (BR-001)
    @Test
    fun `applyDetailsSubmission with changed fingerprint marks STALE`() {
        val state = stateAt(OnboardingStepKey.REVIEW, ValidationStatus.VALID, fingerprint)
        val next = service.applyDetailsSubmission(state, newFingerprint)
        assertEquals(OnboardingStepKey.VALIDATION, next.currentStep)
        assertEquals(ValidationStatus.STALE, next.validationStatus)
        assertEquals(newFingerprint, next.credentialFingerprint)
    }

    // 5. VALID validation allows REVIEW
    @Test
    fun `applyValidationOutcome VALID advances to REVIEW`() {
        val next = service.applyValidationOutcome(pendingValidation(), ProviderValidationOutcome.VALID)
        assertEquals(OnboardingStepKey.REVIEW, next.currentStep)
        assertEquals(ValidationStatus.VALID, next.validationStatus)
        assertTrue(AllowedAction.GO_LIVE in service.allowedActions(next))
    }

    // 6. PARTIAL validation allows REVIEW
    @Test
    fun `applyValidationOutcome PARTIAL advances to REVIEW`() {
        val next = service.applyValidationOutcome(pendingValidation(), ProviderValidationOutcome.PARTIAL)
        assertEquals(OnboardingStepKey.REVIEW, next.currentStep)
        assertEquals(ValidationStatus.PARTIAL, next.validationStatus)
        assertTrue(AllowedAction.GO_LIVE in service.allowedActions(next))
    }

    // 7. INVALID validation returns user to DETAILS
    @Test
    fun `applyValidationOutcome INVALID returns to DETAILS`() {
        val next = service.applyValidationOutcome(pendingValidation(), ProviderValidationOutcome.INVALID)
        assertEquals(OnboardingStepKey.DETAILS, next.currentStep)
        assertEquals(ValidationStatus.INVALID, next.validationStatus)
        assertEquals(setOf(AllowedAction.SUBMIT_DETAILS), service.allowedActions(next))
    }

    // 8. UNAVAILABLE validation remains retryable in VALIDATION
    @Test
    fun `applyValidationOutcome UNAVAILABLE stays in VALIDATION and is retryable`() {
        val next = service.applyValidationOutcome(pendingValidation(), ProviderValidationOutcome.UNAVAILABLE)
        assertEquals(OnboardingStepKey.VALIDATION, next.currentStep)
        assertEquals(ValidationStatus.UNAVAILABLE, next.validationStatus)
        assertTrue(AllowedAction.RETRY_VALIDATION in service.allowedActions(next))
    }

    // 9. TIMEOUT validation remains retryable in VALIDATION
    @Test
    fun `applyValidationOutcome TIMEOUT stays in VALIDATION and is retryable`() {
        val next = service.applyValidationOutcome(pendingValidation(), ProviderValidationOutcome.TIMEOUT)
        assertEquals(OnboardingStepKey.VALIDATION, next.currentStep)
        assertEquals(ValidationStatus.TIMEOUT, next.validationStatus)
        assertTrue(AllowedAction.RETRY_VALIDATION in service.allowedActions(next))
    }

    // 10. Go-live is not allowed unless validation is VALID or PARTIAL
    @Test
    fun `applyGoLive throws when validation is not VALID or PARTIAL`() {
        val state = stateAt(OnboardingStepKey.REVIEW, ValidationStatus.UNAVAILABLE)
        assertThrows<InvalidWorkflowTransitionException> {
            service.applyGoLive(state)
        }
    }

    // 11. Complete session has no allowed actions
    @Test
    fun `complete session has no allowed actions`() {
        val state = stateAt(OnboardingStepKey.COMPLETE, ValidationStatus.VALID)
        assertTrue(service.allowedActions(state).isEmpty())
    }

    // 12. Invalid transition is rejected
    @Test
    fun `applyGoLive from VALIDATION step is rejected`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.VALID)
        assertThrows<InvalidWorkflowTransitionException> {
            service.applyGoLive(state)
        }
    }

    // --- New tests for strengthened lifecycle ---

    // 13. startValidation moves all retryable states to PENDING
    @Test
    fun `startValidation moves retryable statuses to PENDING`() {
        val retryable = listOf(
            ValidationStatus.NOT_STARTED,
            ValidationStatus.STALE,
            ValidationStatus.INVALID,
            ValidationStatus.UNAVAILABLE,
            ValidationStatus.TIMEOUT
        )
        for (status in retryable) {
            val state = stateAt(OnboardingStepKey.VALIDATION, status)
            val next = service.startValidation(state)
            assertEquals(ValidationStatus.PENDING, next.validationStatus, "Expected PENDING for $status")
        }
    }

    // 14. startValidation rejects non-VALIDATION steps
    @Test
    fun `startValidation rejects from non-VALIDATION step`() {
        val state = stateAt(OnboardingStepKey.DETAILS)
        assertThrows<InvalidWorkflowTransitionException> {
            service.startValidation(state)
        }
    }

    // 15. startValidation rejects already PENDING
    @Test
    fun `startValidation rejects when already PENDING`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.PENDING)
        assertThrows<InvalidWorkflowTransitionException> {
            service.startValidation(state)
        }
    }

    // 16. startValidation rejects VALID
    @Test
    fun `startValidation rejects VALID status`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.VALID)
        assertThrows<InvalidWorkflowTransitionException> {
            service.startValidation(state)
        }
    }

    // 17. startValidation rejects PARTIAL
    @Test
    fun `startValidation rejects PARTIAL status`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.PARTIAL)
        assertThrows<InvalidWorkflowTransitionException> {
            service.startValidation(state)
        }
    }

    // 18. applyValidationOutcome rejects non-VALIDATION step
    @Test
    fun `applyValidationOutcome rejects from non-VALIDATION step`() {
        val state = stateAt(OnboardingStepKey.COMPLETE, ValidationStatus.PENDING)
        assertThrows<InvalidWorkflowTransitionException> {
            service.applyValidationOutcome(state, ProviderValidationOutcome.VALID)
        }
    }

    // 19. applyValidationOutcome rejects when status is not PENDING
    @Test
    fun `applyValidationOutcome rejects when status is not PENDING`() {
        val state = stateAt(OnboardingStepKey.VALIDATION, ValidationStatus.NOT_STARTED)
        assertThrows<InvalidWorkflowTransitionException> {
            service.applyValidationOutcome(state, ProviderValidationOutcome.VALID)
        }
    }

    // 20. applyDetailsSubmission rejects COMPLETE
    @Test
    fun `applyDetailsSubmission rejects completed session`() {
        val state = stateAt(OnboardingStepKey.COMPLETE, ValidationStatus.VALID)
        assertThrows<InvalidWorkflowTransitionException> {
            service.applyDetailsSubmission(state, newFingerprint)
        }
    }

    // 21. applyGoLive preserves validation status
    @Test
    fun `applyGoLive preserves last validation status`() {
        val state = stateAt(OnboardingStepKey.REVIEW, ValidationStatus.VALID)
        val next = service.applyGoLive(state)
        assertEquals(OnboardingStepKey.COMPLETE, next.currentStep)
        assertEquals(ValidationStatus.VALID, next.validationStatus)
    }

    // 22. AllowedActionPolicy.resolve is the callable entry point
    @Test
    fun `AllowedActionPolicy resolve returns correct actions for new session`() {
        val state = stateAt(OnboardingStepKey.DETAILS, ValidationStatus.NOT_STARTED, null)
        val actions = AllowedActionPolicy.resolve(state)
        assertEquals(setOf(AllowedAction.SUBMIT_DETAILS), actions)
    }
}
