package com.qualitara.itinera.api

import com.qualitara.itinera.api.dto.SessionResponse
import com.qualitara.itinera.api.dto.SubmitDetailsRequest
import com.qualitara.itinera.golive.GoLiveService
import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStateRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStatus
import com.qualitara.itinera.internal.persistence.repository.OnboardingSessionRepository
import com.qualitara.itinera.internal.persistence.repository.OnboardingStepStateRepository
import com.qualitara.itinera.provider.ProviderValidationService
import com.qualitara.itinera.workflow.AllowedActionPolicy
import com.qualitara.itinera.workflow.CredentialFingerprint
import com.qualitara.itinera.workflow.OnboardingWorkflowService
import com.qualitara.itinera.workflow.ValidationStatus
import com.qualitara.itinera.workflow.model.WorkflowSessionState
import com.qualitara.itinera.workflow.payload.DetailsPayload
import com.qualitara.itinera.workflow.payload.ValidationPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Application service for the onboarding REST API.
 *
 * Orchestrates: session creation, details submission, validation, and go-live.
 * Assembles the full [SessionResponse] from persisted state for every operation.
 *
 * Owns:
 * - HTTP request → domain operation coordination
 * - Full session response DTO assembly (including redaction of sensitive fields)
 * - Session-not-found detection prior to delegating to domain services
 *
 * Does not own:
 * - Workflow transition rules ([OnboardingWorkflowService])
 * - Provider validation execution ([ProviderValidationService])
 * - Go-live transaction ([GoLiveService])
 * - HTTP request/response mapping ([OnboardingController])
 * - SQL execution (repositories)
 */
@Service
class OnboardingApplicationService(
    private val sessionRepo: OnboardingSessionRepository,
    private val stepStateRepo: OnboardingStepStateRepository,
    private val workflowService: OnboardingWorkflowService,
    private val providerValidationService: ProviderValidationService,
    private val goLiveService: GoLiveService,
    private val payloadMapper: JsonbPayloadMapper
) {

    @Transactional
    fun createSession(): SessionResponse {
        val sessionId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        sessionRepo.insert(OnboardingSessionRecord(
            id = sessionId,
            currentStep = OnboardingStepKey.DETAILS,
            status = OnboardingSessionStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        ))
        log.debug { "Session created: $sessionId" }
        return assembleResponse(sessionId)
    }

    fun getSession(sessionId: UUID): SessionResponse {
        sessionRepo.findById(sessionId) ?: throw SessionNotFoundException(sessionId)
        return assembleResponse(sessionId)
    }

    @Transactional
    fun submitDetails(sessionId: UUID, request: SubmitDetailsRequest): SessionResponse {
        val session = sessionRepo.findById(sessionId)
            ?: throw SessionNotFoundException(sessionId)

        val existingFingerprint = stepStateRepo
            .findBySessionIdAndStepKey(sessionId, OnboardingStepKey.DETAILS)
            ?.let { payloadMapper.fromJsonString(it.payloadJson, DetailsPayload::class.java).credentialFingerprint }

        val validationStatus = stepStateRepo
            .findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?.let { payloadMapper.fromJsonString(it.payloadJson, ValidationPayload::class.java).status }
            ?: ValidationStatus.NOT_STARTED

        val currentState = WorkflowSessionState(
            sessionId = sessionId,
            currentStep = session.currentStep,
            validationStatus = validationStatus,
            credentialFingerprint = existingFingerprint
        )

        val newFingerprint = CredentialFingerprint.compute(request.accountId, request.apiKey)
        val nextState = workflowService.applyDetailsSubmission(currentState, newFingerprint)

        val now = OffsetDateTime.now()
        stepStateRepo.upsert(OnboardingStepStateRecord(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            stepKey = OnboardingStepKey.DETAILS,
            status = OnboardingStepStatus.COMPLETED,
            payloadJson = payloadMapper.toJsonString(DetailsPayload(
                companyName = request.companyName,
                accountId = request.accountId,
                apiKeyPresent = true,
                apiKeyMasked = maskApiKey(request.apiKey),
                credentialFingerprint = newFingerprint
            )),
            payloadVersion = 1,
            createdAt = now,
            updatedAt = now,
            completedAt = now
        ))

        // BR-001: if credentials changed, mark the stored validation payload STALE so the
        // DB is authoritative on the current validation status after this request completes.
        if (nextState.validationStatus == ValidationStatus.STALE) {
            stepStateRepo.upsert(OnboardingStepStateRecord(
                id = UUID.randomUUID(),
                sessionId = sessionId,
                stepKey = OnboardingStepKey.VALIDATION,
                status = OnboardingStepStatus.IN_PROGRESS,
                payloadJson = payloadMapper.toJsonString(ValidationPayload(status = ValidationStatus.STALE)),
                payloadVersion = 1,
                createdAt = now,
                updatedAt = now
            ))
        }

        if (nextState.currentStep != session.currentStep) {
            sessionRepo.updateCurrentStepAndStatus(sessionId, nextState.currentStep, OnboardingSessionStatus.DRAFT)
        }

        log.debug { "Details submitted for session=$sessionId, step=${nextState.currentStep}, validationStatus=${nextState.validationStatus}" }
        return assembleResponse(sessionId)
    }

    fun triggerValidation(sessionId: UUID, apiKey: String): SessionResponse {
        sessionRepo.findById(sessionId) ?: throw SessionNotFoundException(sessionId)
        log.debug { "Validation triggered for session=$sessionId" }
        providerValidationService.validate(sessionId, apiKey)
        return assembleResponse(sessionId)
    }

    fun goLive(sessionId: UUID): SessionResponse {
        sessionRepo.findById(sessionId) ?: throw SessionNotFoundException(sessionId)
        log.debug { "Go-live triggered for session=$sessionId" }
        goLiveService.goLive(sessionId)
        return assembleResponse(sessionId)
    }

    private fun assembleResponse(sessionId: UUID): SessionResponse {
        val session = sessionRepo.findById(sessionId)!!

        val detailsSummary = stepStateRepo
            .findBySessionIdAndStepKey(sessionId, OnboardingStepKey.DETAILS)
            ?.let { payloadMapper.fromJsonString(it.payloadJson, DetailsPayload::class.java) }
            ?.let { p ->
                SessionResponse.DetailsSummary(
                    companyName = p.companyName,
                    accountId = p.accountId,
                    apiKeyPresent = p.apiKeyPresent,
                    apiKeyMasked = p.apiKeyMasked
                )
            }

        val validationPayload = stepStateRepo
            .findBySessionIdAndStepKey(sessionId, OnboardingStepKey.VALIDATION)
            ?.let { payloadMapper.fromJsonString(it.payloadJson, ValidationPayload::class.java) }

        val validationStatus = validationPayload?.status ?: ValidationStatus.NOT_STARTED

        val validationSummary = SessionResponse.ValidationSummary(
            status = validationStatus.name,
            items = validationPayload?.items?.map {
                SessionResponse.ValidationSummary.ProviderItemDto(it.externalId, it.name, it.status)
            } ?: emptyList(),
            warnings = validationPayload?.warnings ?: emptyList(),
            reason = validationPayload?.reason
        )

        val state = WorkflowSessionState(
            sessionId = sessionId,
            currentStep = session.currentStep,
            validationStatus = validationStatus,
            credentialFingerprint = null
        )

        return SessionResponse(
            sessionId = session.id,
            currentStep = session.currentStep.name,
            sessionStatus = session.status.name,
            validationStatus = validationStatus.name,
            details = detailsSummary,
            validation = validationSummary,
            allowedActions = AllowedActionPolicy.resolve(state).map { it.name }
        )
    }

    private fun maskApiKey(apiKey: String): String =
        if (apiKey.length <= 4) "*".repeat(apiKey.length)
        else "*".repeat(apiKey.length - 4) + apiKey.takeLast(4)
}
