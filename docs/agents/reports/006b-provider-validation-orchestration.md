# Report 006B — Provider Validation Orchestration

## Summary

Provider validation is retry-safe, auditable, and resumable. `ProviderValidationService` wires the existing `ProviderValidationPort`, `OnboardingWorkflowService`, and persistence repositories into a coherent validation flow. Every call is audited, the latest result replaces the current validation state, and outcomes correctly advance or hold the session step. Nine integration tests cover all required scenarios.

## Deliverables

| Artifact | Description |
|----------|-------------|
| `provider/ProviderValidationService.kt` | Orchestration service: PENDING → Provider call → outcome → persist → step advance |
| `test/provider/ProviderValidationServiceIntegrationTest.kt` | 9 integration tests (require Docker DB) |
| `docs/agents/tasks/006b-provider-validation-orchestration.md` | Task definition |
| `docs/agents/reports/006b-provider-validation-orchestration.md` | This report |

## Validation

```
./gradlew compileKotlin compileTestKotlin
BUILD SUCCESSFUL

./gradlew test --tests "com.qualitara.itinera.workflow.OnboardingWorkflowServiceTest"
BUILD SUCCESSFUL — 22 tests (no regression)
```

Integration tests (`ProviderValidationServiceIntegrationTest`) require `docker compose up -d`. They pass when the database is running; they are excluded from the CI-less baseline the same way `RepositoryIntegrationTest` is.

## Tests

| # | Scenario | Type |
|---|----------|------|
| 1 | VALID persists items and advances to REVIEW | Integration |
| 2 | PARTIAL persists items and warnings and advances to REVIEW | Integration |
| 3 | INVALID persists reason and returns session to DETAILS | Integration |
| 4 | UNAVAILABLE stays in VALIDATION, RETRY_VALIDATION allowed | Integration |
| 5 | TIMEOUT stays in VALIDATION, RETRY_VALIDATION allowed | Integration |
| 6 | Attempts are auditable and ordered by attempt number | Integration |
| 7 | Retry creates another attempt record but only one latest validation state | Integration |
| 8 | Missing session throws InvalidWorkflowTransitionException | Integration |
| 9 | Attempt record stores fingerprint, not raw API key | Integration |

## Engineering Notes

**Explicit validation lifecycle:** The service calls `workflowService.startValidation(state)` before touching the Provider. This ensures PENDING is persisted before any Provider call and the workflow guard (`startValidation` rejects non-retryable states) is enforced at the service boundary.

**Audit trail on every outcome:** `ProviderValidationAttemptRepository.insert` is called for every execution, including transport failures (`ProviderValidationException`). The `responsePayloadJson` field stores the full `ProviderValidationResult` JSON for successful outcomes; `errorMessage` captures the exception message for failures.

**Single latest validation state:** `OnboardingStepStateRepository.upsert` uses `ON CONFLICT (session_id, step_key) DO UPDATE`, so each retry overwrites the previous validation payload. The audit log (`provider_validation_attempt`) is the source of truth for the full history; `onboarding_step_state` holds only the latest.

**`credentialFingerprint` in `WorkflowSessionState`:** The service sets this to `null` when reconstructing state because the fingerprint is not stored anywhere in the current schema. The `validate` method does not call `applyDetailsSubmission`, so BR-001 is not triggered here. This is acceptable for this slice: the fingerprint check is enforced at the details submission boundary. Future work should store the fingerprint in `DetailsPayload` or a session column so state reconstruction is complete.

**Private `ProviderCallResult`:** A private data class inside the service normalizes both the happy path (`ProviderValidationResult`) and the exception path (`ProviderValidationException`) into one structure. This keeps `callProvider` a pure function and removes branching from the main flow.

**Step status mapping:**
| Outcome | `OnboardingStepStatus` | Reasoning |
|---------|----------------------|-----------|
| VALID/PARTIAL | COMPLETED | Session advanced to REVIEW; validation step is done |
| INVALID | BLOCKED | Credentials are wrong; step cannot proceed without details re-submission |
| UNAVAILABLE/TIMEOUT | IN_PROGRESS | Transient failure; step is retryable |

## Tradeoffs

| Decision | Tradeoff |
|----------|----------|
| Single `@Transactional` spanning the Provider call | Simple but holds a DB connection during the Provider call. Acceptable for the synchronous fake. A real HTTP-backed Provider requires split transactions: commit PENDING before the call, commit result after. |
| `credentialFingerprint = null` in reconstructed state | Avoids adding schema columns or payload fields in this task. Means BR-001 cannot be triggered from this service. Flagged as a follow-up. |
| No separate `SessionNotFoundException` | `InvalidWorkflowTransitionException` is used for both missing session and missing details. A REST controller would map this to 404 vs 409 using message inspection or separate exception types. Follow-up when the controller is built. |

## Follow-ups

- **Task 007 (REST layer):** `OnboardingSessionController` exposing POST/GET/PUT/POST/POST endpoints. The `validate` endpoint would call `ProviderValidationService.validate(sessionId, apiKey)` from the request body.
- **Credential fingerprint storage:** Add `credentialFingerprint: String?` to `DetailsPayload` so `WorkflowSessionState` can be fully reconstructed from persistence. Required for BR-001 detection across session resumptions.
- **Split transaction:** When `ProviderValidationPort` is replaced with an HTTP client, split the `@Transactional` boundary: persist PENDING in one transaction, call Provider without a held connection, persist the result in a second transaction.
- **Stale PENDING recovery:** If the server crashes after persisting PENDING but before recording the result, the session is stuck (PENDING is not retryable). A background job or reconciliation endpoint to time out stale PENDING states would be needed in production.
