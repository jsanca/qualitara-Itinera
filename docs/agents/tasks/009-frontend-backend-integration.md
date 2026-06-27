# Task 009 — Frontend Backend Integration

## Context

Project: Itinera

The backend REST API is now implemented and tested. The frontend wizard skeleton already exists and is currently driven by mock session data.

This task replaces mock data with real backend API calls and implements reload-resume behavior.

## Goal

Make the React wizard drive the full onboarding flow through the backend.

## Engineering Capability Added

Frontend wizard integrated with backend-owned onboarding state.

## Scope

Implement:

* API client functions in `frontend/src/api/onboardingApi.ts`
* session bootstrap using `localStorage`
* backend-driven rendering from `currentStep`, `validationStatus`, and `allowedActions`
* details submission
* validation/retry
* review and go-live
* loading/error states

## API Endpoints

Use the implemented backend endpoints:

```text
POST /api/onboarding/sessions
GET  /api/onboarding/sessions/{sessionId}
PUT  /api/onboarding/sessions/{sessionId}/details
POST /api/onboarding/sessions/{sessionId}/validation
POST /api/onboarding/sessions/{sessionId}/go-live
```

## Behavior

### On Page Load

1. Read `itinera.sessionId` from `localStorage`.
2. If present, call `GET /api/onboarding/sessions/{sessionId}`.
3. If not present or if the backend returns 404, call `POST /api/onboarding/sessions`.
4. Save the returned `sessionId` to `localStorage`.
5. Render from the returned session state.

### Details Step

Submit:

```json
{
  "companyName": "...",
  "accountId": "...",
  "apiKey": "..."
}
```

Rules:

* do not store apiKey in frontend after submit
* render `apiKeyMasked` / `apiKeyPresent` from backend response
* update UI from returned session response

### Validation Step

Submit:

```json
{
  "apiKey": "..."
}
```

Rules:

* validation/retry button should call backend validation endpoint
* API key is required because raw credentials are not persisted
* after validation, render returned backend state
* clearly show:

  * PENDING
  * VALID
  * PARTIAL
  * INVALID
  * UNAVAILABLE
  * TIMEOUT
  * STALE
* show warnings for PARTIAL
* allow safe retry when backend allowed actions include retry/start validation

### Review Step

Render:

* company details summary
* provider items
* partial warnings if present
* go-live button gated by `allowedActions`

Go live:

* call backend go-live endpoint
* render COMPLETE/LIVE response

## Constraints

Do not:

* invent workflow transitions locally
* store raw API key in localStorage
* expose credential fingerprint
* add auth
* add routing unless necessary
* add UI libraries
* implement Provider logic in frontend
* add complex state management

## Error Handling

Display a simple user-visible error banner.

If backend returns an error DTO:

```json
{
  "code": "...",
  "message": "..."
}
```

show the message.

Keep console logging minimal and avoid sensitive values.

## Validation

Run:

```bash
cd frontend
npm run build
```

Manual browser validation:

1. Load app with empty localStorage.
2. Confirm new session is created.
3. Refresh page.
4. Confirm session resumes.
5. Submit details with `accountId=valid`.
6. Validate.
7. Go live.
8. Confirm COMPLETE/LIVE state.

Also manually test:

* `partial`
* `invalid`
* `unavailable`
* `timeout`

## Documentation

Create:

```text
docs/agents/tasks/009-frontend-backend-integration.md
docs/agents/reports/009-frontend-backend-integration.md
```

Update:

```text
AI_LOG.md
frontend/README.md
README.md if frontend run instructions changed
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

Capability:

```text
Frontend wizard now drives the onboarding flow through backend state.
```

## Success Criteria

* Reload resumes from backend state.
* Frontend uses backend `currentStep`.
* Frontend uses backend `allowedActions`.
* Raw API key is not persisted in frontend storage.
* User can retry validation safely.
* PARTIAL warnings are visible.
* VALID/PARTIAL can go live.
* Build passes.
