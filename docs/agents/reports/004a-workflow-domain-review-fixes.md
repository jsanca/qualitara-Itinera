# Report 004A — Workflow Domain Review Fixes and Internal Boundary Cleanup

## Summary

Workflow domain model stabilized before API integration. Five domain model corrections were applied to resolve inconsistencies found during review: explicit validation lifecycle (PENDING required before outcomes), guard on completed session re-entry, credential change correctly returns to VALIDATION not DETAILS, `AllowedActionCalculator` replaced with a stateless policy object, and persistence moved under an explicit internal package boundary. Documentation was updated to match the actual state machine.

## Deliverables

| Artifact | Description |
|----------|-------------|
| `internal/persistence/json/*` | Moved from `persistence/json/` — same classes, new internal package |
| `internal/persistence/model/*` | Moved from `persistence/model/` — same classes, new internal package |
| `internal/persistence/repository/*` | Moved from `persistence/repository/` — same classes, new internal package |
| `workflow/AllowedActionPolicy.kt` | Replaces `AllowedActionCalculator`; now a pure `object` with `resolve()` method |
| `workflow/OnboardingWorkflowService.kt` | Added `startValidation`; strengthened `applyValidationOutcome` (PENDING required); guarded `applyDetailsSubmission` on COMPLETE; preserved validation status in `applyGoLive` |
| `test/workflow/OnboardingWorkflowServiceTest.kt` | Expanded from 12 to 22 tests; all validation outcome tests now go through `startValidation` first |
| `test/internal/persistence/repository/RepositoryIntegrationTest.kt` | Moved to internal package; updated imports |
| `README.md`, `docs/ARCHITECTURE.md`, `docs/adr/0002-backend-owned-static-workflow.md` | Corrected state machine diagrams and workflow rules |

## Validation

```
./gradlew compileKotlin compileTestKotlin
BUILD SUCCESSFUL

./gradlew test --tests "com.qualitara.itinera.workflow.OnboardingWorkflowServiceTest"
BUILD SUCCESSFUL — 22 tests, 0 failures, 0 skipped
```

DB-dependent tests (`RepositoryIntegrationTest`, `ItineraApplicationTests`) require Docker and are excluded from this task.

## Tests

| # | Scenario | Result |
|---|----------|--------|
| 1–12 | All original workflow scenarios (updated to use startValidation) | PASS |
| 13 | startValidation moves all retryable statuses to PENDING | PASS |
| 14 | startValidation rejects from non-VALIDATION step | PASS |
| 15 | startValidation rejects when already PENDING | PASS |
| 16 | startValidation rejects VALID status | PASS |
| 17 | startValidation rejects PARTIAL status | PASS |
| 18 | applyValidationOutcome rejects from non-VALIDATION step | PASS |
| 19 | applyValidationOutcome rejects when status is not PENDING | PASS |
| 20 | applyDetailsSubmission rejects completed session | PASS |
| 21 | applyGoLive preserves last validation status | PASS |
| 22 | AllowedActionPolicy.resolve returns correct actions | PASS |

## Engineering Notes

**Validation lifecycle:** The explicit `startValidation → PENDING → applyValidationOutcome` protocol prevents silent state skips where an outcome could be applied without a recorded start. This is not just defensive coding — the REST API layer (next task) will need to persist the PENDING state before calling the Provider. Without the explicit start, there is no persistence window for the in-flight status.

**REVIEW → VALIDATION (not REVIEW → DETAILS):** The original documentation incorrectly said "edit details" returns to DETAILS. The actual implementation returns to VALIDATION (with STALE status) via BR-001. DETAILS is the first-submission step only. Re-editing credentials after validation changes only the fingerprint comparison result, not the step navigation target. This was a documentation bug, not a code bug.

**`AllowedActionPolicy` as `object`:** No Spring injection is needed since the policy has no I/O dependencies. The `@Component` annotation on `AllowedActionCalculator` was unnecessary overhead. Making it a Kotlin `object` communicates the intent clearly and removes the injection from `OnboardingWorkflowService`. This also makes test setup simpler: `OnboardingWorkflowService()` with no constructor arguments.

**Package boundary:** `com.qualitara.itinera.internal.persistence.*` communicates that these classes are implementation details of the persistence adapter, not public domain types. The workflow package references `OnboardingStepKey` and `ProviderValidationOutcome` from the internal package because those enums are the mapping layer between persistence outcomes and workflow decisions. A future refactor could introduce separate domain enums, but that abstraction is premature here.

**Completed session guard:** BR-002 (implicit) — a completed onboarding session must not be reopened by re-submitting details. Credential rotation after go-live is a future separate workflow. Adding this guard makes the COMPLETE state a true terminal in the state machine.

## Tradeoffs

| Decision | Tradeoff |
|----------|----------|
| Move persistence to `internal.persistence` | All workflow imports now reference internal package. Slightly more verbose but explicitly signals boundary intent. |
| `object AllowedActionPolicy` (no `@Component`) | Cannot be mocked by Spring test infrastructure without extra work. Acceptable because it is a pure function; tests call it directly. |
| Keep `OnboardingStepKey` in internal package (not a separate domain enum) | Avoids mapping boilerplate for now. If the domain ever needs different step semantics than persistence, this will need revisiting. |
| PENDING guard on `applyValidationOutcome` | Adds one required call (`startValidation`) to the validation path. The REST layer must call two operations instead of one. Justified by correctness: PENDING state must be persisted before the Provider is called to survive concurrent failures. |

## Follow-ups

- **Task 005:** REST API layer — `OnboardingSessionController` with POST/GET/PUT/POST/POST endpoints. Will use `startValidation` → call Provider → `applyValidationOutcome` as the validation flow.
- **Task 006:** Provider port and `FakeProviderValidationClient` wiring deterministic outcomes to `ProviderValidationOutcome`.
- **Separate domain enums:** If `OnboardingStepKey` or `ProviderValidationOutcome` need to diverge from their persistence representations, introduce domain-layer enums with a mapping layer. Deferred until API slice proves the need.
