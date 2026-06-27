# Task 006C — Provider Validation Review Fixes

## Context

Project: Itinera

Phase 4 implemented Provider validation orchestration and was reviewed by Deep.

The review approved the slice with minor changes, but identified one important domain completeness issue before REST integration:

* `WorkflowSessionState.credentialFingerprint` cannot currently be reconstructed from persistence.

This task fixes that issue and performs two small cleanup/documentation improvements before Phase 5.

## Goal

Stabilize Provider validation before REST API integration.

## Scope

### 1. Persist Credential Fingerprint in Details Payload

Update `DetailsPayload` to include an internal credential fingerprint field.

Suggested field:

```kotlin id="f8vg7h"
credentialFingerprint: String?
```

Rules:

* fingerprint is computed from `accountId + apiKey`
* raw `apiKey` must never be persisted
* raw `apiKey` must never be returned
* raw `apiKey` must never be logged
* fingerprint is persisted only so `WorkflowSessionState` can be reconstructed after reload/restart
* fingerprint must not be included in frontend-facing response DTOs later

Update any tests that construct `DetailsPayload`.

Update validation orchestration so reconstructed `WorkflowSessionState` uses the persisted fingerprint instead of `null`.

### 2. Move ProviderValidationException

Move `ProviderValidationException` out of the fake client file.

Create:

```text id="uoilnk"
backend/src/main/kotlin/com/qualitara/itinera/provider/ProviderValidationException.kt
```

Reason:

The exception belongs to the Provider port contract, not to the fake implementation.

### 3. Add Transaction Boundary Warning

Add KDoc to `ProviderValidationService` explaining:

```text id="d39g7g"
The current @Transactional boundary is acceptable for the in-process fake Provider.
When ProviderValidationPort is replaced by a real HTTP client, the transaction must be split:
1. persist PENDING in one transaction
2. call the Provider outside a DB transaction
3. persist the result/audit record in a second transaction
```

Do not refactor the transaction boundary in this task.

### 4. Optional Test

If straightforward, add a test proving that:

* `DetailsPayload.credentialFingerprint` is persisted
* reconstructed workflow state uses the stored fingerprint
* BR-001 can work after session reload/resume

Do not overbuild this.

## Out of Scope

Do not:

* implement REST endpoints
* implement frontend wizard
* change Provider fake behavior
* split transactions
* add mock HTTP Provider service
* add dynamic workflow
* change database schema unless absolutely necessary
* expose credential fingerprint in API contract
* log fingerprint or API key

## Validation

Run:

```bash id="e121el"
cd backend
./gradlew test
```

If integration tests require DB:

```bash id="wvej4l"
docker compose up -d postgres
cd backend
./gradlew test
```

## Documentation

Create:

```text id="8r4wfd"
docs/agents/tasks/006c-provider-validation-review-fixes.md
docs/agents/reports/006c-provider-validation-review-fixes.md
```

Update:

```text id="9z80w1"
AI_LOG.md
docs/agents/reports/006b-provider-validation-orchestration.md if needed
docs/API_CONTRACT.md only if it currently implies fingerprint is returned
```

## Report Requirements

Use standard report structure:

* Summary
* Deliverables
* Validation
* Tests
* Engineering Notes
* Tradeoffs
* Follow-ups

Capability:

```text id="cyghfn"
Provider validation state reconstruction completed.
```

## Success Criteria

* `WorkflowSessionState` can be reconstructed with credential fingerprint from persisted details.
* BR-001 is not silently skipped after resume.
* Provider exception is part of the port contract, not the fake implementation.
* Transaction warning is documented.
* Tests pass.
