# Task 007 — Transactional Go Live

## Context

Project: Itinera

The persistence model, workflow domain, and Provider validation orchestration are now in place.

This phase implements the final backend transition: taking a validated or partially validated onboarding session live.

This task does not implement REST controllers yet. It creates the service capability that REST will call.

## Goal

Implement a transactional, idempotent go-live service.

The service must:

* allow go-live only after validation is `VALID` or `PARTIAL`
* create a `partner_account` exactly once
* mark the onboarding session `LIVE`
* move the workflow step to `COMPLETE`
* avoid half-committed state
* make repeated go-live calls safe

## Engineering Capability Added

Transactional and idempotent go-live transition.

## Scope

Create a service such as:

```text
GoLiveService
```

or, if the existing application-service structure already suggests a better name, use that.

The service should orchestrate:

* `OnboardingSessionRepository`
* `OnboardingStepStateRepository`
* `PartnerAccountRepository`
* `OnboardingWorkflowService`

## Required Behavior

### Preconditions

Go-live is allowed only when:

```text
currentStep = REVIEW
validationStatus = VALID or PARTIAL
```

Reject go-live when:

* session does not exist
* validation step state does not exist
* validation status is missing
* validation status is `NOT_STARTED`
* validation status is `PENDING`
* validation status is `STALE`
* validation status is `INVALID`
* validation status is `UNAVAILABLE`
* validation status is `TIMEOUT`
* session is not in `REVIEW`
* session is already complete but inconsistent

### Idempotency

Calling go-live twice must not create duplicate `partner_account` records.

If the session is already:

```text
currentStep = COMPLETE
status = LIVE
```

and a `partner_account` already exists for the session, return the completed state without creating another account.

Use the existing unique constraint:

```text
partner_account.session_id unique
```

as a structural safeguard.

The service should still make the idempotency behavior explicit instead of relying only on constraint exceptions.

### Transactionality

Use Spring transaction management.

Annotate the service method with:

```kotlin
@Transactional
```

The transaction should include:

1. reading session state
2. reading latest validation step payload
3. creating or reusing partner account
4. marking session complete/live

No partial state should be committed if an exception occurs before completion.

### Validation Payload

Read the latest `VALIDATION` step state payload.

Deserialize it into:

```text
ValidationPayload
```

Use the existing JSONB mapper.

The payload status is the source for go-live eligibility.

### Company Name

Read the latest `DETAILS` step payload.

Deserialize it into:

```text
DetailsPayload
```

Use `companyName` to create `partner_account`.

Do not expose or log:

* raw API key
* credential fingerprint
* full details payload

### Workflow

Use `OnboardingWorkflowService.applyGoLive(...)` to enforce domain rules.

Do not duplicate workflow transition logic in the go-live service.

The service should reconstruct a `WorkflowSessionState` from persisted session and validation payload, then call the workflow service.

### Logging

Use Kotlin logging.

Rules:

* debug log normal go-live attempt and successful completion using `sessionId`
* warn log rejected go-live transitions using `sessionId` and reason
* never log raw payloads
* never log credential fingerprints
* never log API keys

### Internal API / KDoc

Add KDoc to the service explaining:

* this service owns the transactional go-live use case
* it does not own Provider validation
* it does not own REST response shaping
* it relies on repositories for persistence and workflow service for transition rules

## Tests

Add integration tests requiring PostgreSQL.

Minimum tests:

1. `VALID` validation can go live.
2. `PARTIAL` validation can go live.
3. Go-live creates one partner account.
4. Calling go-live twice does not create a duplicate partner account.
5. Go-live marks session status `LIVE`.
6. Go-live marks current step `COMPLETE`.
7. Go-live rejected for `INVALID`.
8. Go-live rejected for `UNAVAILABLE`.
9. Go-live rejected for `TIMEOUT`.
10. Go-live rejected for `PENDING`.
11. Go-live rejected when validation step is missing.
12. Go-live rejected when details step is missing.
13. Repeated go-live returns completed state if already live and partner account exists.

If test setup becomes large, create clear helper methods.

Do not reduce coverage of previous tests.

## Error Handling

Use existing domain exceptions if appropriate.

If the current exception types are too generic, create focused exceptions such as:

```text
GoLiveRejectedException
OnboardingSessionNotFoundException
```

But do not overbuild an exception hierarchy.

REST mapping can be refined in the REST API phase.

## Out of Scope

Do not:

* implement REST controllers
* implement frontend
* call Provider
* split Provider validation transactions
* add auth
* add dynamic workflow
* change database schema unless absolutely necessary
* expose credential fingerprint
* log sensitive data

## Documentation

Create:

```text
docs/agents/tasks/007-transactional-go-live.md
docs/agents/reports/007-transactional-go-live.md
```

Update:

```text
AI_LOG.md
docs/PLAN.md if phase status is tracked
docs/FUTURE_FORWARDS.md if new production concerns appear
```

If relevant, update:

```text
docs/API_CONTRACT.md
```

only if the service behavior changes a previously documented go-live rule.

## Validation

Run:

```bash
docker compose up -d postgres
cd backend
./gradlew test
```

Also run workflow-only tests if useful:

```bash
./gradlew test --tests "com.qualitara.itinera.workflow.OnboardingWorkflowServiceTest"
```

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
Transactional go-live transition established.
```

## Success Criteria

* Go-live succeeds for `VALID`.
* Go-live succeeds for `PARTIAL`.
* Go-live fails for every non-eligible validation state.
* Go-live is idempotent.
* `partner_account` is not duplicated.
* Session is marked `LIVE`.
* Workflow step is marked `COMPLETE`.
* Transaction boundary prevents half-committed state.
* Tests pass.
