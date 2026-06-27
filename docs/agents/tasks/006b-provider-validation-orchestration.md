#Task 006B — Provider Validation Orchestration

## Goal

Wire Provider validation into onboarding state.

Validation must be retry-safe, auditable, and resumable.

## Scope

Implement a service such as:

* `ProviderValidationService`

Responsibilities:

1. Load session and details state.
2. Call workflow `startValidation`.
3. Persist `PENDING` validation state.
4. Call `ProviderValidationPort`.
5. Record `provider_validation_attempt`.
6. Apply validation outcome through workflow.
7. Persist latest validation result into `onboarding_step_state`.
8. Return updated session/workflow state.

## Required Behavior

* VALID persists items and moves toward REVIEW.
* PARTIAL persists items + warnings and allows REVIEW.
* INVALID persists reason and returns user to DETAILS/SUBMIT_DETAILS flow.
* UNAVAILABLE/TIMEOUT persist retryable state.
* Repeated validation creates new audit attempts.
* Latest validation result replaces current validation step state.
* Raw API key must never be logged or returned.

## Tests

Add tests proving:

* valid result persists latest validation payload
* partial persists warnings
* invalid persists reason
* unavailable/timeout are retryable
* validation attempts are auditable and ordered
* retry creates another attempt but one latest validation state

## Constraints

Do not implement REST controller yet.
Do not implement frontend.
Do not implement go-live.
Use Spring transactions where needed.
No business logic in repositories.

## Report

Create:

* `docs/agents/tasks/006b-provider-validation-orchestration.md`
* `docs/agents/reports/006b-provider-validation-orchestration.md`

Capability:

`Provider validation is retry-safe, auditable, and resumable.`
