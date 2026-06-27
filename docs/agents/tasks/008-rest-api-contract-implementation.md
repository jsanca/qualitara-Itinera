# Task 008 — REST API Contract Implementation

## Context

Project: Itinera

The backend now has:

* persistence model
* workflow domain
* Provider validation orchestration
* transactional go-live service
* API contract draft in `docs/API_CONTRACT.md`

This task implements the REST API layer so the frontend can drive the onboarding flow entirely from backend-owned state.

## Goal

Expose the onboarding workflow through REST endpoints.

The frontend must be able to:

1. create or resume a session
2. get the current session state
3. submit details
4. trigger Provider validation
5. go live

Every successful endpoint should return the same full session response shape.

## Engineering Capability Added

REST API contract implemented for the onboarding flow.

## API Endpoints

Implement:

```text id="o3yqhb"
POST /api/onboarding/sessions
GET  /api/onboarding/sessions/{sessionId}
PUT  /api/onboarding/sessions/{sessionId}/details
POST /api/onboarding/sessions/{sessionId}/validation
POST /api/onboarding/sessions/{sessionId}/go-live
```

Follow:

```text id="4s07g7"
docs/API_CONTRACT.md
```

If the implementation needs to differ from the contract, update the contract in the same task and explain the reason in the report.

## Response Principle

Every successful mutation returns the full session representation.

The frontend should not need to infer transitions locally.

Response should include:

```text id="djvb8l"
sessionId
currentStep
sessionStatus
validationStatus
details
validation
allowedActions
```

## DTOs

Create request/response DTOs for the API layer.

Suggested packages:

```text id="8ipd9z"
com.qualitara.itinera.api
com.qualitara.itinera.api.dto
```

### Create Session

Request:

```text id="3hr3ew"
none
```

Response:

```text id="f9glyj"
full session response
```

Initial state:

```text id="wtszeh"
currentStep = DETAILS
sessionStatus = DRAFT
validationStatus = NOT_STARTED
allowedActions = [SUBMIT_DETAILS]
```

### Get Session

Returns full session response.

### Submit Details

Request:

```json id="4pab8q"
{
  "companyName": "Acme Inc.",
  "accountId": "valid",
  "apiKey": "secret"
}
```

Rules:

* compute credential fingerprint from `accountId + apiKey`
* persist `DetailsPayload` including:

    * companyName
    * accountId
    * apiKeyPresent = true
    * apiKeyMasked
    * credentialFingerprint
* never return raw `apiKey`
* never return credential fingerprint
* if credentials changed after validation, BR-001 must invalidate validation and return to `VALIDATION` with `STALE`

Response:

```text id="hj8htx"
full session response
```

### Trigger Validation

Request:

```json id="cmdk3k"
{
  "apiKey": "secret"
}
```

Rationale:

The raw API key is not returned by the API. Validation needs a supplied key unless/until encrypted credential storage is implemented.

Rules:

* call `ProviderValidationService`
* use persisted `accountId` from details
* use request `apiKey`
* provider outcomes are represented as normal workflow states, not HTTP errors:

    * VALID
    * PARTIAL
    * INVALID
    * UNAVAILABLE
    * TIMEOUT

Response:

```text id="9i3agx"
full session response
```

### Go Live

Request:

```text id="7calmh"
none
```

Rules:

* call `GoLiveService`
* valid only for latest validation `VALID` or `PARTIAL`
* idempotent

Response:

```text id="fzbt2b"
full session response
```

## Application Service

Create an application service if useful, such as:

```text id="irv4ow"
OnboardingApplicationService
```

Responsibilities:

* create session
* load session response
* submit details
* trigger validation
* go live
* assemble full session response DTO

This service may orchestrate:

* repositories
* `OnboardingWorkflowService`
* `ProviderValidationService`
* `GoLiveService`
* JSONB mapper

The controller should remain thin.

## Session Response Assembly

Create a component responsible for rebuilding response state from persistence.

Suggested name:

```text id="7oh9al"
OnboardingSessionResponseAssembler
```

Responsibilities:

* load `onboarding_session`
* load details step state
* load validation step state
* deserialize payloads
* compute allowed actions from workflow state
* redact sensitive fields
* return API DTO

Do not expose:

* raw API key
* credential fingerprint
* full internal JSON payloads

## Error Responses

Implement consistent error response shape:

```json id="0bnms1"
{
  "code": "INVALID_TRANSITION",
  "message": "Cannot go live before validation succeeds."
}
```

Handle at least:

```text id="s69qch"
SESSION_NOT_FOUND
INVALID_TRANSITION
BAD_REQUEST
UNSUPPORTED_PAYLOAD_VERSION
```

Map:

* missing session → 404
* invalid transition → 409
* malformed request → 400
* unsupported payload version → 422

If existing exceptions are too generic, add focused exceptions only where useful.

Do not overbuild a complex exception hierarchy.

## Logging

Use Kotlin logging.

Controller/application service logs:

* debug for accepted operations
* warn for rejected invalid transitions or bad requests

Never log:

* raw API key
* credential fingerprint
* full payloads

## Package Documentation / KDoc

Add KDoc/package docs for API/application packages:

* API layer owns HTTP contract
* application service owns orchestration and DTO assembly
* workflow service owns transition rules
* repositories own persistence
* provider service owns Provider validation orchestration
* go-live service owns transactional go-live

## Tests

Add controller/application tests.

Minimum tests:

1. Create session returns `DETAILS`, `DRAFT`, `NOT_STARTED`, `SUBMIT_DETAILS`.
2. Get session returns persisted state.
3. Submit details stores redacted details and never returns raw apiKey.
4. Submit details computes/persists fingerprint but does not return it.
5. Submit details after prior validation changes credentials and returns `STALE`.
6. Validation endpoint handles `valid`.
7. Validation endpoint handles `partial` and returns warnings.
8. Validation endpoint handles `invalid` and returns reason.
9. Validation endpoint handles `unavailable`.
10. Validation endpoint handles `timeout`.
11. Go-live succeeds after `valid`.
12. Go-live succeeds after `partial`.
13. Go-live rejected before validation.
14. Repeated go-live is idempotent.
15. Unknown session returns 404.
16. Invalid transition returns consistent error DTO.

Prefer MockMvc/SpringBootTest integration-style tests over pure controller unit tests, because this layer exists to prove the contract across services.

## Validation

Run:

```bash id="5rm4z0"
docker compose up -d postgres
cd backend
./gradlew test
```

Also manually verify if time allows:

```bash id="t4gz28"
curl -X POST http://localhost:8080/api/onboarding/sessions
```

## Documentation

Create:

```text id="jkwj3g"
docs/agents/tasks/008-rest-api-contract-implementation.md
docs/agents/reports/008-rest-api-contract-implementation.md
```

Update:

```text id="g620cx"
AI_LOG.md
docs/API_CONTRACT.md if needed
backend/README.md with curl examples if practical
README.md if run/test instructions changed
```

## Out of Scope

Do not:

* implement frontend integration
* implement auth
* implement real Provider HTTP service
* implement encrypted credential storage
* implement dynamic workflow
* add JPA
* add OpenAPI codegen
* add production observability infrastructure
* expose credential fingerprint
* expose raw API key

## Report Requirements

Use the standard report structure:

* Summary
* Deliverables
* Validation
* Tests
* Engineering Notes
* Tradeoffs
* Follow-ups

Capability:

```text id="vgpm06"
REST API contract implemented for the onboarding flow.
```

## Success Criteria

* Frontend can drive the full flow using backend state only.
* API returns current step, validation status, details summary, validation summary, and allowed actions.
* Raw API key is never returned.
* Credential fingerprint is never returned.
* Provider outcomes are reflected as session state.
* Invalid transitions return consistent error responses.
* Go-live endpoint is idempotent.
* Tests pass.
