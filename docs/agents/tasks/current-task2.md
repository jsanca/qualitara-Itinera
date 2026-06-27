# Mini Task 009A — Backend End-to-End Flow Tests

## Context

Project: Itinera

The backend has integration tests for persistence, Provider validation, go-live, and REST endpoints. This task adds a small number of end-to-end backend tests that prove the full REST flow works across layers.

Do not change production code unless a test exposes a real bug.

## Goal

Add backend integration tests for complete onboarding flows.

## Engineering Capability Added

Backend full-flow contract verified end to end.

## Scope

Create or update a test class such as:

```text
backend/src/test/kotlin/com/qualitara/itinera/api/OnboardingFlowIntegrationTest.kt
```

Use `MockMvc` and the real PostgreSQL-backed Spring context.

## Required Tests

### 1. Happy Path — VALID

Flow:

```text
POST /api/onboarding/sessions
PUT  /api/onboarding/sessions/{id}/details  accountId=valid
POST /api/onboarding/sessions/{id}/validation
POST /api/onboarding/sessions/{id}/go-live
```

Assert final response:

```text
currentStep = COMPLETE
sessionStatus = LIVE
validationStatus = VALID
allowedActions = []
```

Also assert:

* raw apiKey is never returned
* partner account exists only once if repository access is available

### 2. PARTIAL Can Go Live

Same flow with:

```text
accountId=partial
```

Assert:

* warnings are returned after validation
* go-live succeeds
* final session is COMPLETE/LIVE

### 3. UNAVAILABLE Then Retry VALID

Flow:

```text
create session
submit details with accountId=unavailable
validate → UNAVAILABLE
submit details with accountId=valid and same/new apiKey
validate → VALID
go-live → COMPLETE/LIVE
```

Assert:

* unavailable response includes retry action
* retry produces successful validation
* session can go live afterward

### 4. INVALID Blocks Go Live

Flow:

```text
create session
submit details accountId=invalid
validate → INVALID
go-live → rejected
```

Assert:

* HTTP 409
* error code INVALID_TRANSITION
* session is not LIVE

## Constraints

Do not:

* add frontend code
* add new endpoints
* change API contract
* add production features
* overbuild test helpers

Keep tests readable.

## Validation

Run:

```bash
docker compose up -d postgres
cd backend
./gradlew test
```

## Documentation

Create:

```text
docs/agents/tasks/009a-backend-e2e-flow-tests.md
docs/agents/reports/009a-backend-e2e-flow-tests.md
```

Update:

```text
AI_LOG.md
```

## Report Requirements

Use the standard report structure.

Capability:

```text
Full backend onboarding flows verified end to end.
```

## Success Criteria

* Complete valid flow passes.
* Complete partial flow passes.
* Unavailable → retry → valid flow passes.
* Invalid blocks go-live.
* All backend tests pass.
