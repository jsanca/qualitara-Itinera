# Report 008 — REST API Contract Implementation

## Summary

REST API contract implemented for the onboarding flow. The frontend can now drive the full partner onboarding wizard using backend-owned state: create a session, submit credentials, trigger validation, and go live — with every response returning the authoritative session state including `currentStep`, `validationStatus`, `details`, `validation`, and `allowedActions`. Raw API keys and credential fingerprints are never returned.

## Deliverables

- **`api/dto/SessionResponse.kt`** — canonical response DTO with nested `DetailsSummary`, `ValidationSummary`, and `ProviderItemDto`.
- **`api/dto/SubmitDetailsRequest.kt`** — request DTO with `@NotBlank` field validation.
- **`api/dto/TriggerValidationRequest.kt`** — request DTO with `@NotBlank` on `apiKey`.
- **`api/dto/ErrorResponse.kt`** — uniform error shape (`code`, `message`).
- **`api/SessionNotFoundException.kt`** — maps missing sessions to HTTP 404, distinct from `InvalidWorkflowTransitionException` (409).
- **`api/OnboardingApplicationService.kt`** — application service orchestrating all five operations and assembling the `SessionResponse` from persistence.
- **`api/OnboardingController.kt`** — thin REST controller for all five endpoints.
- **`api/GlobalExceptionHandler.kt`** — `@RestControllerAdvice` mapping `SessionNotFoundException` → 404, `InvalidWorkflowTransitionException` → 409, `UnsupportedPayloadVersionException` → 422, Bean Validation failures and `HttpMessageNotReadableException` → 400.
- **`build.gradle.kts`** — added `spring-boot-starter-validation` for `@NotBlank`.
- **`docs/API_CONTRACT.md`** — updated validate endpoint to document the `apiKey` request body (see Engineering Notes).
- **`test/api/OnboardingApiIntegrationTest.kt`** — 19 integration tests (see Tests section).

## Validation

- `./gradlew compileKotlin` — clean
- `./gradlew compileTestKotlin` — clean
- `./gradlew test` — **85 tests across 7 classes, 0 failures, 0 errors**

Manual smoke test (requires `bootRun`):
```bash
curl -s -X POST http://localhost:8080/api/onboarding/sessions | jq .
```

## Tests

All 19 tests in `OnboardingApiIntegrationTest` use `MockMvc` with `@AutoConfigureMockMvc` and hit the full stack including PostgreSQL.

| # | Test |
|---|------|
| 1 | Create session returns correct initial state |
| 2 | Create session sets `Location` header |
| 3 | Get session returns persisted state |
| 4 | Submit details — raw API key never returned, apiKeyPresent=true, masking correct |
| 5 | Submit details advances step to VALIDATION |
| 6 | Submit details after valid validation with changed credentials returns STALE (BR-001) |
| 7 | Validation — valid outcome advances to REVIEW with items |
| 8 | Validation — partial outcome returns items and warnings |
| 9 | Validation — invalid outcome returns session to DETAILS with reason |
| 10 | Validation — unavailable stays in VALIDATION with RETRY_VALIDATION action |
| 11 | Validation — timeout stays in VALIDATION with RETRY_VALIDATION action |
| 12 | Go-live succeeds after valid validation |
| 13 | Go-live succeeds after partial validation |
| 14 | Go-live rejected before validation succeeds (409) |
| 15 | Repeated go-live is idempotent (one partner account, second call succeeds) |
| 16 | Unknown session returns 404 with SESSION_NOT_FOUND |
| 17 | Invalid transition returns 409 with INVALID_TRANSITION |
| 18 | Missing required field in submit-details returns 400 with MALFORMED_REQUEST |
| 19 | Missing request body for validation returns 400 with MALFORMED_REQUEST |

## Engineering Notes

**Contract divergence — validation endpoint request body.** The original `docs/API_CONTRACT.md` said the validate endpoint has no body. The task specification correctly identified a gap: the raw API key cannot be reconstructed from persistence (it is not stored), so each validation call must supply it. The contract was updated to document `{ "apiKey": "..." }` as required for POST `/validation`. The rationale is documented in the contract.

**BR-001 STALE persistence.** When `applyDetailsSubmission` detects a credential change and returns `ValidationStatus.STALE`, the application service also upserts the VALIDATION step state payload to `{ "status": "STALE" }`. Without this, `assembleResponse()` would read the stale (but not invalidated) VALID payload from the DB and return the wrong status. The workflow service computes the new state; the application service persists it.

**`SessionNotFoundException` vs `InvalidWorkflowTransitionException`.** The same domain exception was being used for "session not found" (404) and "invalid workflow state" (409). A dedicated `SessionNotFoundException` is introduced at the API layer. The application service pre-checks session existence and throws it; domain services continue to throw `InvalidWorkflowTransitionException` for their internal guards (which never fire in practice since the pre-check runs first).

**Response assembly from persistence.** `assembleResponse()` always reads fresh from the DB rather than projecting from a service return value. This ensures the response is consistent with what was committed and avoids drift between the returned `WorkflowSessionState` and the stored `ValidationPayload` (which can contain items/warnings that the workflow state does not carry).

**Error message safety.** `InvalidWorkflowTransitionException` messages contain internal info (session IDs, step names). The exception handler substitutes a fixed safe message: "The requested action is not valid for the current session state." The `code` field gives the frontend a stable value to program against.

## Tradeoffs

- `credentialFingerprint` is never returned in the API response. The `DetailsSummary` DTO is a projection of `DetailsPayload` that explicitly omits it. This matches the contract requirement; the fingerprint is an internal domain value, not a partner-facing field.
- The validate and go-live endpoints call `assembleResponse(sessionId)` after the underlying service completes, which does additional DB reads. The alternative (project from the returned `WorkflowSessionState`) would miss `items`, `warnings`, and `reason` from the VALIDATION payload, requiring extra state threading. The extra reads are acceptable for this system.
- Error messages for `InvalidWorkflowTransitionException` are fixed/generic rather than context-specific. This is safe (no internal info leaks) but gives less user-facing detail. A richer error hierarchy or problem-detail implementation is a future concern.

## Follow-ups

**Frontend integration (Phase 6 / Task 009):** Wire the React wizard to these endpoints. The frontend reads `currentStep` and `allowedActions` to drive step rendering and button enablement. Session ID is stored in `localStorage`; on reload, `GET /sessions/{sessionId}` restores state.
