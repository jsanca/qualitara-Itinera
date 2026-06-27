# Report 005B — API Contract Draft

## Summary

Backend/frontend API contract drafted before REST implementation. The planned onboarding endpoints now share one authoritative session response shape, define write-only API key handling, enumerate validation lifecycle states, and distinguish Provider workflow outcomes from HTTP errors.

## Deliverables

- **`docs/API_CONTRACT.md`** — planned REST endpoints, common session DTO, request/response examples, validation statuses, allowed actions, and error semantics.
- **Create and resume contract** — a new session starts at `DETAILS` with `SUBMIT_DETAILS`; `GET` returns the same complete representation used by mutations.
- **Details contract** — request accepts `companyName`, `accountId`, and write-only `apiKey`; responses expose only `apiKeyPresent` and optional `apiKeyMasked`.
- **Validation contract** — documents all eight workflow statuses and treats Provider outcomes as successful, backend-owned state changes.
- **Go-live contract** — completes at `currentStep = COMPLETE`, marks the session `LIVE`, preserves the successful validation status, and returns no allowed actions.
- **Error contract** — consistent `{ code, message }` shape with mappings for malformed requests, missing sessions, unsupported payload versions, and invalid transitions.
- **Documentation index updates** — README link and AI task-log entry added.

## Validation

```text
Documentation links checked against repository paths.
Endpoint list checked against the task scope.
JSON examples parsed successfully.
Task-owned changes checked to confirm this task changes documentation only.
```

## Tests

No application tests were run because this task changes documentation only and implements no runtime behavior.

## Engineering Notes

**One session representation.** Create, read, details submission, validation, and go-live all return the same full shape. This gives the frontend one hydration path and makes a mutation response sufficient to render the next screen.

**Provider failures versus protocol failures.** `INVALID`, `UNAVAILABLE`, and `TIMEOUT` are represented as successful session responses because they are expected workflow outcomes. HTTP error responses are reserved for malformed requests, missing resources, unsupported versions, and actions forbidden by current state.

**Payload versioning.** Versioning remains a backend persistence concern rather than a frontend-supplied request field. `UNSUPPORTED_PAYLOAD_VERSION` covers a session whose stored step state cannot be read by the running API version.

**State naming.** The API uses `sessionStatus` rather than the older generic `status` example so it remains distinct from `validationStatus` and `validation.status`. Go-live uses `COMPLETE` for the workflow step and `LIVE` for the session lifecycle, matching the active domain model.

## Tradeoffs

The top-level `validationStatus` duplicates `validation.status`. The duplication is intentional: the former is a compact workflow discriminator, while the latter keeps the validation summary self-describing. The contract requires them to be equal.

The contract specifies HTTP status and error-code mappings before controller implementation. Implementers may refine message wording, but changing codes, response fields, or status mappings should be treated as a contract change shared with the frontend.

## Follow-ups

- Implement request/response DTOs and REST controllers against this contract.
- Align the frontend placeholder types with the final DTO names and uppercase allowed-action values.
- Add controller tests for response shape, API key redaction, errors, and transition behavior.
- Implement Provider validation behind its port without changing the public outcome model.
