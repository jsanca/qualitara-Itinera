# Itinera Onboarding API Contract

## Scope

This is the implemented HTTP/JSON contract between the React wizard and Spring Boot backend. The backend is authoritative: the frontend renders `currentStep`, `validationStatus`, and `allowedActions` and never infers transitions locally.

All successful endpoints return the full session representation. JSON requests use `Content-Type: application/json`.

## Endpoints

| Method | Path | Request | Success |
|---|---|---|---|
| `POST` | `/api/onboarding/sessions` | no body | `201 Created` |
| `GET` | `/api/onboarding/sessions/{sessionId}` | no body | `200 OK` |
| `PUT` | `/api/onboarding/sessions/{sessionId}/details` | details JSON | `200 OK` |
| `POST` | `/api/onboarding/sessions/{sessionId}/validation` | API-key JSON | `200 OK` |
| `POST` | `/api/onboarding/sessions/{sessionId}/go-live` | no body | `200 OK` |

`sessionId` is a UUID. Create also returns `Location: /api/onboarding/sessions/{sessionId}`.

## Full Session Response

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "REVIEW",
  "sessionStatus": "DRAFT",
  "validationStatus": "PARTIAL",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "partial",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "PARTIAL",
    "items": [
      {
        "externalId": "feed-001",
        "name": "Primary Feed",
        "status": "active"
      }
    ],
    "warnings": [
      "Secondary Feed is paused",
      "Rate limit at 80% capacity"
    ],
    "reason": null
  },
  "allowedActions": [
    "EDIT_DETAILS",
    "GO_LIVE"
  ]
}
```

| Field | Type | Meaning |
|---|---|---|
| `sessionId` | UUID string | Stable onboarding session identifier. |
| `currentStep` | enum | `DETAILS`, `VALIDATION`, `REVIEW`, or `COMPLETE`. |
| `sessionStatus` | enum | `DRAFT` or `LIVE`. |
| `validationStatus` | enum | Current validation lifecycle status. |
| `details` | object or `null` | Safe details projection; never contains the raw API key or fingerprint. |
| `validation` | object | Latest validation summary. `validation.status` equals `validationStatus`. |
| `allowedActions` | enum array | Complete set of actions currently permitted by backend policy. |

Lists are always arrays. Missing details are `null`. `reason` and `apiKeyMasked` may be `null`.

Allowed action values:

- `SUBMIT_DETAILS`
- `EDIT_DETAILS`
- `START_VALIDATION`
- `RETRY_VALIDATION`
- `GO_TO_REVIEW`
- `GO_LIVE`

The frontend must not invoke an action absent from `allowedActions`.

Validation status values:

| Status | Meaning |
|---|---|
| `NOT_STARTED` | No attempt has started. |
| `PENDING` | An attempt is in progress inside the validation lifecycle. |
| `VALID` | Provider accepted the credentials and required data. |
| `PARTIAL` | Provider accepted them with warnings or partial data. |
| `INVALID` | Provider rejected the credentials/account. |
| `UNAVAILABLE` | Provider is temporarily unavailable; retry is allowed. |
| `TIMEOUT` | Provider call timed out; retry is allowed. |
| `STALE` | Credentials changed and the previous outcome is no longer trusted. |

`NOT_STARTED`, `PENDING`, and `STALE` are workflow states rather than Provider outcomes. With the current synchronous fake, the validation mutation normally returns a final outcome rather than `PENDING`.

## Create Session

```http
POST /api/onboarding/sessions
```

No request body.

### `201 Created`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "DETAILS",
  "sessionStatus": "DRAFT",
  "validationStatus": "NOT_STARTED",
  "details": null,
  "validation": {
    "status": "NOT_STARTED",
    "items": [],
    "warnings": [],
    "reason": null
  },
  "allowedActions": ["SUBMIT_DETAILS"]
}
```

## Get or Resume Session

```http
GET /api/onboarding/sessions/{sessionId}
```

Returns `200 OK` with the full session shape. This is the resume endpoint; its result supersedes locally cached workflow state.

## Submit Details

```http
PUT /api/onboarding/sessions/{sessionId}/details
Content-Type: application/json
```

### Request

```json
{
  "companyName": "Acme Logistics",
  "accountId": "valid",
  "apiKey": "provider-secret-value"
}
```

All fields are required and must be non-blank.

The raw API key is used to compute a credential fingerprint, then discarded. Stored details contain `apiKeyPresent`, a masked display value, and the fingerprint; responses expose only the first two. The raw API key is never persisted, logged, or returned.

Repeating identical details is safe. If `accountId` or `apiKey` changes after prior validation, the response has `currentStep = VALIDATION` and `validationStatus = STALE`.

### Example `200 OK`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "VALIDATION",
  "sessionStatus": "DRAFT",
  "validationStatus": "NOT_STARTED",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "valid",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "NOT_STARTED",
    "items": [],
    "warnings": [],
    "reason": null
  },
  "allowedActions": ["EDIT_DETAILS", "START_VALIDATION"]
}
```

## Trigger or Retry Validation

```http
POST /api/onboarding/sessions/{sessionId}/validation
Content-Type: application/json
```

### Request

```json
{
  "apiKey": "provider-secret-value"
}
```

`apiKey` is required and non-blank. It must be supplied again because Itinera does not persist the raw key from details submission. The service uses it only for this Provider call and stores a fingerprint in the audit attempt.

The response is the full session with one of `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, or `TIMEOUT`. These are expected workflow outcomes and return `200 OK`, including transient or rejected results.

The in-process fake selects the outcome using the stored `accountId`: `valid`, `partial`, `invalid`, `unavailable`, or `timeout`; any other value is invalid. This contract does not expose or require a particular future real-Provider transport.

### Example `200 OK` — unavailable

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "VALIDATION",
  "sessionStatus": "DRAFT",
  "validationStatus": "UNAVAILABLE",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "unavailable",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "UNAVAILABLE",
    "items": [],
    "warnings": [],
    "reason": null
  },
  "allowedActions": ["EDIT_DETAILS", "RETRY_VALIDATION"]
}
```

## Go Live

```http
POST /api/onboarding/sessions/{sessionId}/go-live
```

No request body. Go-live is allowed only from `REVIEW` with latest validation `VALID` or `PARTIAL`. Repeating the call after successful completion is idempotent.

### `200 OK`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "COMPLETE",
  "sessionStatus": "LIVE",
  "validationStatus": "VALID",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "valid",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "VALID",
    "items": [
      {
        "externalId": "feed-001",
        "name": "Primary Feed",
        "status": "active"
      },
      {
        "externalId": "feed-002",
        "name": "Secondary Feed",
        "status": "active"
      }
    ],
    "warnings": [],
    "reason": null
  },
  "allowedActions": []
}
```

Completion preserves the last `VALID` or `PARTIAL` status for historical context.

## Errors

Expected client errors use one shape:

```json
{
  "code": "INVALID_TRANSITION",
  "message": "The requested action is not valid for the current session state."
}
```

| HTTP status | Code | Condition |
|---|---|---|
| `400 Bad Request` | `MALFORMED_REQUEST` | Missing/malformed JSON, blank required field, or wrong field type. |
| `404 Not Found` | `SESSION_NOT_FOUND` | Well-formed UUID does not identify a session. |
| `409 Conflict` | `INVALID_TRANSITION` | Action is not allowed by current backend workflow state, including premature go-live. |
| `422 Unprocessable Entity` | `UNSUPPORTED_PAYLOAD_VERSION` | Stored versioned state cannot be read by this application version. |

Examples:

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "Onboarding session was not found."
}
```

```json
{
  "code": "MALFORMED_REQUEST",
  "message": "apiKey is required"
}
```

Error responses and logs must never echo submitted credentials.
