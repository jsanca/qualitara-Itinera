# Report 007 — Transactional Go Live

## Summary

Transactional go-live transition established. `GoLiveService` atomically creates a `partner_account`, advances the session to `COMPLETE`, and marks it `LIVE` — or returns the existing completed state if go-live is called more than once. The service delegates all eligibility rules to `OnboardingWorkflowService.applyGoLive()` and never duplicates transition logic.

## Deliverables

- **`golive/GoLiveService.kt`** — new `@Service` in `com.qualitara.itinera.golive`. Single public method `goLive(sessionId: UUID): WorkflowSessionState`. KDoc documents ownership, idempotency contract, and what it does not own.
- **`internal/persistence/repository/OnboardingSessionRepository.kt`** — added `markLiveAndComplete(id, completedAt)`: sets `current_step=COMPLETE`, `status=LIVE`, `completed_at`, and `updated_at` in one SQL statement, replacing the need to call `updateCurrentStepAndStatus` and `markCompleted` sequentially.
- **`test/golive/GoLiveServiceIntegrationTest.kt`** — 14 integration tests (see Tests section).

## Validation

- `./gradlew compileKotlin` — clean
- `./gradlew compileTestKotlin` — clean
- `./gradlew test` — **66 tests across 6 classes, 0 failures, 0 errors**

## Tests

All 14 tests in `GoLiveServiceIntegrationTest` run against PostgreSQL via `@SpringBootTest`.

| # | Test | Focus |
|---|------|-------|
| 1 | `valid validation advances session to COMPLETE and marks it LIVE` | step, status, completedAt |
| 2 | `partial validation can go live` | PARTIAL eligibility |
| 3 | `go-live creates exactly one partner account with company name from details` | account creation + companyName |
| 4 | `go-live twice does not create a duplicate partner account` | duplicate guard |
| 5 | `repeated go-live on already-live session returns completed state` | idempotent return value |
| 6 | `go-live rejected when validation is INVALID` | rejection |
| 7 | `go-live rejected when validation is UNAVAILABLE` | rejection |
| 8 | `go-live rejected when validation is TIMEOUT` | rejection |
| 9 | `go-live rejected when validation is PENDING` | rejection |
| 10 | `go-live rejected when validation is NOT_STARTED` | rejection |
| 11 | `go-live rejected when validation step state is missing` | missing payload guard |
| 12 | `go-live rejected when details step state is missing` | missing payload guard |
| 13 | `go-live rejected when session does not exist` | session guard |
| 14 | `go-live rejected when session is not in REVIEW step` | step guard |

## Engineering Notes

**`markLiveAndComplete` vs two separate calls.** The existing repository had `updateCurrentStepAndStatus` (sets step + status) and `markCompleted` (sets status + completed_at). Using both in sequence would redundantly write `status=LIVE` twice in one transaction. Adding `markLiveAndComplete` expresses the intent precisely in one SQL statement.

**Idempotency is explicit, not implicit.** The `partner_account.session_id` unique constraint is a structural safeguard against bugs, but it would surface as a DB exception rather than a clean return. `GoLiveService` checks for an existing account before inserting, so a repeated call returns the completed state gracefully. This matches the task requirement: "make the idempotency behavior explicit instead of relying only on constraint exceptions."

**`credentialFingerprint = null` on go-live.** The reconstructed `WorkflowSessionState` sets `credentialFingerprint = null`. At go-live time the fingerprint is irrelevant — no BR-001 check is needed since `applyGoLive()` doesn't inspect it and the session cannot accept more credential submissions after COMPLETE. Loading the fingerprint from `DetailsPayload` was considered but provides no value and exposes internal data unnecessarily.

**Logging.** The service logs debug on entry, idempotent return, and successful completion. Rejection from `applyGoLive()` is caught, warn-logged with the exception message, and re-thrown — this adds observability without duplicating the transition logic. The company name appears in the successful completion log since it is not sensitive.

**Package choice.** `com.qualitara.itinera.golive` mirrors the `provider` package structure. An alternative of putting `GoLiveService` under `workflow` was rejected — the service touches repositories and has transaction management responsibilities that belong at the application service layer, not inside the pure domain.

## Tradeoffs

- No REVIEW step state is written at go-live time. The REVIEW payload (`ReviewPayload`) captures warnings acceptance, which is a future frontend concern. The task scope is the go-live transition itself; writing a REVIEW step state would be premature without a REST endpoint driving it.
- `InvalidWorkflowTransitionException` is reused for all rejection cases (session not found, missing payloads, ineligible state). The task noted that more focused exceptions could be created; they are deferred to the REST phase where HTTP status code mapping will clarify the hierarchy.

## Follow-ups

**008 — REST API Integration (Phase 5):** Wire `OnboardingController` endpoints for session creation, details submission, validate, and go-live. The details submission endpoint will compute and persist `credentialFingerprint` in `DetailsPayload`. Response DTOs will project `WorkflowSessionState` and `allowedActions` for the frontend.
