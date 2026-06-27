# Task 004A — Workflow Domain Review Fixes and Internal Boundary Cleanup

## Context

Project: Itinera

Task 004 implemented the backend-owned workflow domain. Deep review found no implementation-breaking issues, but identified domain-model inconsistencies that should be resolved before building REST APIs on top of the workflow layer.

This task stabilizes the workflow model and cleans up package boundaries.

Do not implement REST controllers yet.

## Goal

Resolve the Phase 3 workflow review findings and make the internal persistence boundary explicit in package structure.

## Scope

### 1. Resolve State Machine Documentation Drift

The implementation treats `DETAILS` as the initial entry step. After details have been submitted, editing credentials does not transition back to `DETAILS`; instead, credential changes invalidate validation and return the session to `VALIDATION` with stale validation state.

Update documentation to reflect this model.

Update:

```text
README.md
docs/ARCHITECTURE.md
docs/adr/0002-backend-owned-static-workflow.md
```

Replace diagrams/rules that say:

```text
REVIEW --> DETAILS: edit details
```

with the actual rule:

```text
REVIEW --> VALIDATION: edit credentials / credentials changed / validation stale
```

Clarify:

```text
DETAILS is the initial data-entry step.
After initial submission, editing details is an action, not necessarily a workflow step.
If credentials change, previous validation becomes stale and the session returns to VALIDATION.
```

### 2. Keep `PENDING` and Add Explicit Start Validation Transition

Keep `ValidationStatus.PENDING`.

Add a workflow method such as:

```kotlin
fun startValidation(state: WorkflowSessionState): WorkflowSessionState
```

Rules:

* allowed only from `currentStep = VALIDATION`
* allowed only when validation status is:

  * `NOT_STARTED`
  * `STALE`
  * `INVALID`
  * `UNAVAILABLE`
  * `TIMEOUT`
* returns state with:

  * `currentStep = VALIDATION`
  * `validationStatus = PENDING`
* rejects:

  * `VALID`
  * `PARTIAL`
  * `PENDING`
  * any non-VALIDATION step

This prepares the synchronous fake Provider flow without pretending validation has no in-progress state.

### 3. Strengthen Validation Outcome Preconditions

Update `applyValidationOutcome`.

Rules:

* only allowed from `currentStep = VALIDATION`
* only allowed when existing validation status is `PENDING`
* rejects applying outcomes directly from:

  * `NOT_STARTED`
  * `STALE`
  * `INVALID`
  * `UNAVAILABLE`
  * `TIMEOUT`
  * `VALID`
  * `PARTIAL`

Intended flow:

```text
START_VALIDATION
→ PENDING
→ applyValidationOutcome(...)
```

### 4. Guard Completed Sessions

Update `applyDetailsSubmission`.

Rules:

* if `currentStep = COMPLETE`, reject with `InvalidWorkflowTransitionException`
* a completed onboarding session must not be reopened by editing credentials
* credential rotation after go-live is a future separate workflow, not part of onboarding

### 5. Preserve Validation Status on Completion

Do not clear validation status on `applyGoLive`.

Document that `COMPLETE` preserves the last validation outcome for historical context.

### 6. Rename Allowed Action Calculator

Rename:

```text
AllowedActionCalculator
```

to:

```text
AllowedActionPolicy
```

Rename method:

```text
calculate(...)
```

to:

```text
resolve(...)
```

Reason:

This component applies domain policy, not numeric calculation.

If it has no dependencies, prefer:

```kotlin
object AllowedActionPolicy
```

instead of a Spring `@Component`.

Do not inject it only for convenience.

### 7. Move Persistence Under an Internal Package Boundary

Current persistence packages are implementation details.

Move persistence-related code under an explicit internal namespace.

Preferred package structure:

```text
com.qualitara.itinera.internal.persistence.json
com.qualitara.itinera.internal.persistence.model
com.qualitara.itinera.internal.persistence.repository
```

Move all existing persistence classes:

```text
persistence/json/*
persistence/model/*
persistence/repository/*
```

to:

```text
internal/persistence/json/*
internal/persistence/model/*
internal/persistence/repository/*
```

Update imports and tests.

Add/update package documentation explaining:

* this is internal infrastructure
* it is not the public application API
* repositories are persistence adapters
* records represent stored rows, not rich domain objects
* workflow/business logic must not move into this package

Do not move workflow packages under internal. Workflow is application/domain behavior and will be used by the API layer.

### 8. Tests

Update or add tests for:

1. `startValidation` moves retryable validation states to `PENDING`.
2. `startValidation` rejects non-VALIDATION steps.
3. `startValidation` rejects already `PENDING`.
4. `startValidation` rejects `VALID` and `PARTIAL`.
5. `applyValidationOutcome` rejects non-VALIDATION step.
6. `applyValidationOutcome` rejects when current validation status is not `PENDING`.
7. `applyValidationOutcome` accepts outcome from `PENDING`.
8. `applyDetailsSubmission` rejects `COMPLETE`.
9. `applyGoLive` preserves last validation status.
10. Allowed action policy uses `resolve`, not `calculate`.

Existing tests should be updated to follow the new explicit validation lifecycle:

```text
VALIDATION + START_VALIDATION -> PENDING -> applyValidationOutcome(...)
```

Run all tests:

```bash
cd backend
./gradlew test
```

## Logging

Keep existing logging discipline:

* debug for normal transitions
* warn for rejected invalid transitions
* never log raw API keys
* never log credential fingerprints
* never log full payloads

Do not add noisy logging to value objects.

## Documentation

Create:

```text
docs/agents/tasks/004a-workflow-domain-review-fixes.md
docs/agents/reports/004a-workflow-domain-review-fixes.md
```

Update:

```text
AI_LOG.md
README.md
docs/ARCHITECTURE.md
docs/adr/0002-backend-owned-static-workflow.md
```

Update package KDoc/package-info documentation if package names move.

## Out of Scope

Do not:

* implement REST controllers
* implement Provider fake
* implement go-live persistence transaction
* add frontend code
* add dynamic workflow engine
* change database schema
* add JPA
* move workflow into persistence
* expose raw API keys

## Report Requirements

Use the standard report structure:

* Summary
* Deliverables
* Validation
* Tests
* Engineering Notes
* Tradeoffs
* Follow-ups

The report should describe the capability:

```text
Workflow domain model stabilized before API integration.
```

## Success Criteria

* Documentation matches the actual workflow state machine.
* `PENDING` has an explicit transition path.
* Validation outcomes can only be applied from `PENDING`.
* Completed sessions cannot be reopened through details submission.
* `AllowedActionPolicy.resolve(...)` replaces calculator naming.
* Persistence is under an explicit internal package boundary.
* All tests pass.
