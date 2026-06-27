# Task 005B — API Contract Draft

## Context

Project: Itinera

The backend workflow domain is being finalized. Before implementing REST controllers, define the API contract the backend and frontend will share.

This is a documentation/design task only.

## Goal

Create a clear REST API contract for the onboarding session workflow. The contract must allow the frontend to render the wizard entirely from backend-owned state.

## Scope

Create `docs/API_CONTRACT.md` and define:

- `POST /api/onboarding/sessions`
- `GET /api/onboarding/sessions/{sessionId}`
- `PUT /api/onboarding/sessions/{sessionId}/details`
- `POST /api/onboarding/sessions/{sessionId}/validation`
- `POST /api/onboarding/sessions/{sessionId}/go-live`

Every successful mutation returns the full session response: `sessionId`, `currentStep`, `sessionStatus`, `validationStatus`, `details`, `validation`, and `allowedActions`.

Document create, get, details submission, validation, and go-live DTO examples. The details request accepts an API key, but responses expose only `apiKeyPresent` and optional `apiKeyMasked`.

Document validation statuses `NOT_STARTED`, `PENDING`, `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, `TIMEOUT`, and `STALE`.

Document simple error responses for unknown sessions, unsupported payload versions, invalid transitions, malformed requests, and go-live before a `VALID` or `PARTIAL` result.

## Constraints

Do not:

- change application code
- implement controllers
- implement Provider behavior
- invent authentication
- document endpoints outside the take-home scope

## Documentation

Create:

```text
docs/API_CONTRACT.md
docs/agents/tasks/005b-api-contract-draft.md
docs/agents/reports/005b-api-contract-draft.md
```

Update:

```text
AI_LOG.md
README.md
```

## Report Requirements

Use the standard report structure:

- Summary
- Deliverables
- Validation
- Tests
- Engineering Notes
- Tradeoffs
- Follow-ups

The report should describe the capability:

```text
Backend/frontend API contract drafted before REST implementation.
```

## Success Criteria

- The frontend can render and resume the wizard from the session response alone.
- Mutation responses carry the complete authoritative session state.
- API keys are write-only and never appear in responses.
- Validation lifecycle and Provider outcomes are unambiguous.
- Expected errors have a small, consistent shape.
- No application code or endpoints are implemented.
