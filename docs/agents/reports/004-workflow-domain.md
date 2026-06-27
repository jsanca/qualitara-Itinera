# Report 004 — Workflow Domain

## Summary

Backend-owned onboarding workflow state machine established. The workflow package now owns step transitions, allowed action calculation, credential-change detection (BR-001), and typed step payload DTOs. Twelve unit tests covering all required scenarios pass without a database.

## Deliverables

| Artifact | Description |
|----------|-------------|
| `workflow/ValidationStatus.kt` | Domain enum extending `ProviderValidationOutcome` with workflow-specific states (NOT_STARTED, STALE, PENDING) |
| `workflow/AllowedAction.kt` | Enum of actions the frontend may offer based on backend state |
| `workflow/CredentialFingerprint.kt` | SHA-256 fingerprint computation (`accountId:apiKey`) for BR-001 |
| `workflow/AllowedActionCalculator.kt` | Pure `@Component` that maps step + validationStatus → Set<AllowedAction> |
| `workflow/OnboardingWorkflowService.kt` | `@Service` owning all workflow transitions; uses kotlin-logging-jvm |
| `workflow/model/WorkflowSessionState.kt` | Domain model for workflow position (step, validationStatus, credentialFingerprint) |
| `workflow/payload/DetailsPayload.kt` | Typed payload for DETAILS step; no raw API key |
| `workflow/payload/ValidationPayload.kt` | Typed payload for VALIDATION step |
| `workflow/payload/ReviewPayload.kt` | Typed payload for REVIEW step |
| `workflow/payload/ProviderItem.kt` | Value object for a single discovered Provider item |
| `workflow/exception/InvalidWorkflowTransitionException.kt` | Thrown on illegal transitions |
| `workflow/exception/UnsupportedPayloadVersionException.kt` | Thrown on unknown payload version |
| `test/workflow/OnboardingWorkflowServiceTest.kt` | 12 pure unit tests, no Spring context |

## Validation

```
./gradlew test --tests "com.qualitara.itinera.workflow.OnboardingWorkflowServiceTest"
BUILD SUCCESSFUL — 12 tests, 0 failures, 0 skipped
```

DB-dependent integration tests (`RepositoryIntegrationTest`, `ItineraApplicationTests`) are excluded from this task's scope and require the Docker database to be running.

## Tests

| # | Scenario | Result |
|---|----------|--------|
| 1 | New session starts at DETAILS | PASS |
| 2 | Submitting details advances to VALIDATION | PASS |
| 3 | Same fingerprint re-submission is idempotent | PASS |
| 4 | Changed fingerprint marks validation STALE (BR-001) | PASS |
| 5 | VALID outcome advances to REVIEW, GO_LIVE allowed | PASS |
| 6 | PARTIAL outcome advances to REVIEW, GO_LIVE allowed | PASS |
| 7 | INVALID outcome returns to DETAILS, SUBMIT_DETAILS allowed | PASS |
| 8 | UNAVAILABLE outcome stays in VALIDATION, RETRY_VALIDATION allowed | PASS |
| 9 | TIMEOUT outcome stays in VALIDATION, RETRY_VALIDATION allowed | PASS |
| 10 | Go-live rejected when validation is not VALID or PARTIAL | PASS |
| 11 | COMPLETE session has no allowed actions | PASS |
| 12 | applyGoLive from VALIDATION step throws | PASS |

## Engineering Notes

**BR-001 implementation:** `applyDetailsSubmission` compares the incoming fingerprint against the stored `credentialFingerprint`. A change (non-null stored fingerprint that differs) triggers step=VALIDATION + status=STALE. First submission (null fingerprint) always advances from DETAILS without triggering BR-001. Re-submission with identical fingerprint preserves the current step, allowing idempotent detail updates from any step without disrupting REVIEW state.

**AllowedActionCalculator as pure component:** The calculator has no state and no I/O. It can be unit-tested without Spring context and is easily replaceable by a future database-backed implementation.

**Logging discipline:** `OnboardingWorkflowService` logs transitions at `debug` level and rejected transitions at `warn`. No API keys, fingerprints, or payload contents appear in any log statement.

**VersionedPayload on all DTOs:** All three payload types implement `VersionedPayload` with `version = 1`. This enables forward-compatible deserialization when schema evolves.

**`VALIDATION + VALID/PARTIAL` edge case:** The `AllowedActionCalculator` returns `GO_TO_REVIEW` for this combination rather than `GO_LIVE`. This handles the case where the service has not yet persisted the step advancement. After `applyValidationOutcome` runs and the step moves to REVIEW, the calculator correctly returns `EDIT_DETAILS + GO_LIVE`.

## Tradeoffs

| Decision | Tradeoff |
|----------|----------|
| State transitions return new `WorkflowSessionState` (immutable copy) | Callers must persist the result; there is no auto-save. Keeps domain logic pure and testable. |
| `applyValidationOutcome` guards the VALIDATION step precondition | Callers who pass an incorrect step get a clear exception rather than silent state corruption. |
| `AllowedActionCalculator` is separate from `OnboardingWorkflowService` | Slightly more indirection, but keeps the calculator independently testable and replaceable. |
| No `GO_TO_REVIEW` from REVIEW step | Once in REVIEW, the partner can only go live or edit. The step does not need an explicit navigation action. |

## Follow-ups

- **Task 005:** REST API layer (`OnboardingSessionController`) to expose `newSession`, `submitDetails`, `validate`, `goLive`, and session read. This is where `WorkflowSessionState` is persisted and `allowedActions` is returned to the frontend.
- **Task 006:** Provider port and in-process fake (`FakeProviderValidationClient`), wiring `applyValidationOutcome` to real Provider outcomes.
- **Testcontainers:** Integration tests that load the Spring context require the database. Testcontainers would allow these to run in CI without a pre-running container.
- **Payload version migration:** `UnsupportedPayloadVersionException` is defined but not yet thrown. The service layer for task 005 will need to handle deserialization of versioned payloads and raise this exception on unrecognized versions.
