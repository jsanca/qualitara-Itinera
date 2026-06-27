# Itinera Onboarding API Contract

## Status and Scope

This document defines the planned HTTP/JSON contract shared by the Itinera backend and frontend. It is a design contract for the take-home onboarding workflow; the endpoints are not implemented yet.

The API does not define authentication, a real Provider integration, or endpoints outside the onboarding session workflow.

## Contract Principles

- The backend is the source of truth for workflow state and transitions.
- The frontend renders `currentStep`, `validationStatus`, and `allowedActions`; it does not infer transitions locally.
- Every successful mutation returns the full, authoritative session response.
- A Provider API key is accepted when details are submitted but is never returned in a response.
- Provider validation outcomes are workflow results, not HTTP errors. `INVALID`, `UNAVAILABLE`, and `TIMEOUT` therefore return a successful session response.
- JSON requests and responses use `Content-Type: application/json`.

## Endpoints

| Method | Path | Purpose | Success |
|---|---|---|---|
| `POST` | `/api/onboarding/sessions` | Create a new onboarding session | `201 Created` |
| `GET` | `/api/onboarding/sessions/{sessionId}` | Read or resume a session | `200 OK` |
| `PUT` | `/api/onboarding/sessions/{sessionId}/details` | Submit or replace partner details | `200 OK` |
| `POST` | `/api/onboarding/sessions/{sessionId}/validation` | Start or retry Provider validation | `200 OK` |
| `POST` | `/api/onboarding/sessions/{sessionId}/go-live` | Complete onboarding and make the partner live | `200 OK` |

`sessionId` is a UUID. A successful create response should also set `Location: /api/onboarding/sessions/{sessionId}`.

## Session Response

All successful endpoints return the same session representation. Fields are not conditionally omitted; unavailable details are represented by `null`, and lists are represented by empty arrays.

| Field | Type | Meaning |
|---|---|---|
| `sessionId` | UUID string | Stable onboarding session identifier. |
| `currentStep` | enum | `DETAILS`, `VALIDATION`, `REVIEW`, or `COMPLETE`. |
| `sessionStatus` | enum | `DRAFT` while onboarding is active; `LIVE` after go-live. |
| `validationStatus` | enum | Current validation lifecycle status. |
| `details` | object or `null` | Safe details summary; never contains the raw API key. |
| `validation` | object | Latest validation summary. Its `status` equals `validationStatus`. |
| `allowedActions` | enum array | Complete set of transitions the backend currently permits. |

Allowed action values are:

- `SUBMIT_DETAILS`
- `EDIT_DETAILS`
- `START_VALIDATION`
- `RETRY_VALIDATION`
- `GO_TO_REVIEW`
- `GO_LIVE`

The frontend must only offer or invoke actions present in `allowedActions`. An empty array means the workflow has no further onboarding action.

### Details Summary

```json
{
  "companyName": "Acme Logistics",
  "accountId": "acct-12345",
  "apiKeyPresent": true,
  "apiKeyMasked": "********wxyz"
}
```

`apiKeyMasked` is optional and may be `null`. It is display-only and must not contain enough information to reconstruct the credential. `apiKeyPresent` is the authoritative indication that a credential is stored.

### Validation Summary

```json
{
  "status": "PARTIAL",
  "items": [
    {
      "externalId": "feed-42",
      "name": "Primary shipment feed",
      "status": "AVAILABLE"
    }
  ],
  "warnings": [
    "One optional feed is unavailable."
  ],
  "reason": null
}
```

`items` and `warnings` are always arrays. `reason` is optional and may be `null`; it supplies a safe, user-facing explanation when one is available.

Validation status values are:

| Status | Meaning |
|---|---|
| `NOT_STARTED` | No validation attempt has started. |
| `PENDING` | A validation attempt is in progress. |
| `VALID` | Credentials and required Provider data are valid. |
| `PARTIAL` | Validation succeeded with warnings or partial Provider data. |
| `INVALID` | The Provider rejected the credentials or required data. |
| `UNAVAILABLE` | The Provider is temporarily unavailable; retry is allowed. |
| `TIMEOUT` | The Provider call timed out; retry is allowed. |
| `STALE` | Credentials changed, so an earlier result can no longer be trusted. |

`STALE`, `NOT_STARTED`, and `PENDING` are workflow lifecycle states. They are not Provider outcomes.

## Create Session

```http
POST /api/onboarding/sessions
```

The request has no body. The backend creates a new draft session at the initial details step.

### Response — `201 Created`

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
  "allowedActions": [
    "SUBMIT_DETAILS"
  ]
}
```

## Get Session

```http
GET /api/onboarding/sessions/{sessionId}
```

Returns `200 OK` with the same full session response shape as create and every mutation. The frontend uses this endpoint to resume a saved session after reload; the returned state supersedes any locally cached workflow state.

## Submit Details

```http
PUT /api/onboarding/sessions/{sessionId}/details
```

### Request

```json
{
  "companyName": "Acme Logistics",
  "accountId": "acct-12345",
  "apiKey": "provider-secret-value"
}
```

All three fields are required. Payload versioning belongs to the backend persistence boundary and is not supplied by the frontend.

The API key is write-only: it is accepted in this request and must never appear in a response, error, or log. The response replaces it with `apiKeyPresent` and, optionally, `apiKeyMasked`.

Submitting the same details is safe to repeat and replaces the stored details state. If `accountId` or `apiKey` changes after a prior validation, the backend sets validation to `STALE`, returns to `VALIDATION`, and requires a new validation before go-live. The frontend follows the returned state rather than predicting this transition.

### Response — `200 OK`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "VALIDATION",
  "sessionStatus": "DRAFT",
  "validationStatus": "NOT_STARTED",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "acct-12345",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "NOT_STARTED",
    "items": [],
    "warnings": [],
    "reason": null
  },
  "allowedActions": [
    "EDIT_DETAILS",
    "START_VALIDATION"
  ]
}
```

## Validate Integration

```http
POST /api/onboarding/sessions/{sessionId}/validation
```

The request has no body. This endpoint starts an initial validation or retries a retryable result. It will eventually call the Provider behind the backend's validation port; this contract does not define Provider behavior or its implementation.

The returned full session reflects one of the Provider outcomes: `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, or `TIMEOUT`. A `PENDING` session representation may be observed while an attempt is in progress. Validation attempts must not expose the submitted API key.

### Example Response — `200 OK`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "REVIEW",
  "sessionStatus": "DRAFT",
  "validationStatus": "PARTIAL",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "acct-12345",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "PARTIAL",
    "items": [
      {
        "externalId": "feed-42",
        "name": "Primary shipment feed",
        "status": "AVAILABLE"
      }
    ],
    "warnings": [
      "One optional feed is unavailable."
    ],
    "reason": null
  },
  "allowedActions": [
    "EDIT_DETAILS",
    "GO_LIVE"
  ]
}
```

`INVALID`, `UNAVAILABLE`, and `TIMEOUT` also return `200 OK` with the corresponding `validationStatus`, validation summary, current step, and backend-computed recovery actions.

## Go Live

```http
POST /api/onboarding/sessions/{sessionId}/go-live
```

The request has no body. Go-live is permitted only from `REVIEW` when the latest validation status is `VALID` or `PARTIAL`.

### Response — `200 OK`

```json
{
  "sessionId": "7d648f95-3a6f-4f65-9c65-b31d62439ee2",
  "currentStep": "COMPLETE",
  "sessionStatus": "LIVE",
  "validationStatus": "PARTIAL",
  "details": {
    "companyName": "Acme Logistics",
    "accountId": "acct-12345",
    "apiKeyPresent": true,
    "apiKeyMasked": "********alue"
  },
  "validation": {
    "status": "PARTIAL",
    "items": [
      {
        "externalId": "feed-42",
        "name": "Primary shipment feed",
        "status": "AVAILABLE"
      }
    ],
    "warnings": [
      "One optional feed is unavailable."
    ],
    "reason": null
  },
  "allowedActions": []
}
```

Completion preserves the last successful `VALID` or `PARTIAL` validation status for historical context.

## Error Responses

All errors use one simple shape:

```json
{
  "code": "INVALID_TRANSITION",
  "message": "Cannot go live before validation succeeds."
}
```

| HTTP status | Code | When returned |
|---|---|---|
| `400 Bad Request` | `MALFORMED_REQUEST` | JSON is invalid, required fields are missing, or field values have the wrong type or format. |
| `404 Not Found` | `SESSION_NOT_FOUND` | `sessionId` is well-formed but no onboarding session exists. |
| `409 Conflict` | `INVALID_TRANSITION` | The requested action is not valid for the session's backend-owned state. |
| `409 Conflict` | `INVALID_TRANSITION` | Go-live is requested before the latest validation is `VALID` or `PARTIAL`. |
| `422 Unprocessable Entity` | `UNSUPPORTED_PAYLOAD_VERSION` | Stored versioned session state cannot be read by this API version. |

Example unknown session:

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "Onboarding session was not found."
}
```

Example unsupported payload version:

```json
{
  "code": "UNSUPPORTED_PAYLOAD_VERSION",
  "message": "Details payload version 2 is not supported."
}
```

Example malformed request:

```json
{
  "code": "MALFORMED_REQUEST",
  "message": "companyName is required."
}
```

Error messages must be safe to show to a user and must never echo an API key or other submitted credential.
