# ADR 0003 — Hybrid Relational + JSONB Step State

## Status

Accepted

## Context

The onboarding flow must be resumable. Each step has different data:

- **Details:** company name, account id, API key presence.
- **Validation:** status, items, warnings, invalid reason, transient failure details.
- **Review:** go-live readiness confirmation.

Two modeling options were considered:

1. **Fully relational** — separate columns or tables for every step payload field. Strong constraints, but schema changes required for every future step or payload field addition.
2. **Hybrid relational + JSONB** — relational columns for lifecycle and identity; JSONB for per-step payload. More flexible; fewer field-level DB constraints.

## Decision

Use a hybrid model:

- `onboarding_session` stores relational lifecycle fields with typed PostgreSQL enum columns.
- `onboarding_step_state` stores step identity and status relationally; the step payload is in a `jsonb` column.
- `provider_validation_attempt` stores attempt identity and outcome relationally; the Provider response is in a `jsonb` column.
- `partner_account` is fully relational — no JSONB needed for the final live record.

Core tables:

```text
onboarding_session
  id, current_step (enum), status (enum), created_at, updated_at, completed_at

onboarding_step_state
  id, session_id, step_key (enum), status (enum), payload (jsonb), payload_version,
  created_at, updated_at, completed_at
  UNIQUE (session_id, step_key)

provider_validation_attempt
  id, session_id, attempt_number, account_id, request_fingerprint,
  outcome (enum), response_payload (jsonb), error_message, started_at, completed_at
  UNIQUE (session_id, attempt_number)

partner_account
  id, session_id UNIQUE, company_name, status (enum), went_live_at, created_at
```

## Type Safety Strategy

Repositories do not pass untyped maps through the codebase. Each step payload has a typed Kotlin DTO:

- `DetailsPayload`
- `ValidationPayload`
- `ReviewPayload`

DTOs implement `VersionedPayload` (see ADR 0004). The service layer validates and serializes/deserializes. `JsonbPayloadMapper` handles all JSONB conversion.

## Consequences

**Benefits:**
- Lifecycle columns (`current_step`, `status`, `completed_at`) are typed, indexable, and DB-enforced.
- Unique constraints on `(session_id, step_key)` support upsert-based idempotency without application-level locking.
- Step payloads can evolve without schema migrations for every new field.
- PostgreSQL JSONB supports GIN indexes if future queries need to filter on payload fields.

**Tradeoffs:**
- PostgreSQL enforces fewer constraints inside JSONB. Application-level validation via Kotlin DTOs compensates.
- JSONB payloads are harder to inspect or query without understanding the application schema.
- Payload compatibility across versions must be managed explicitly (addressed in ADR 0004).

## Future Forward

- Add JSON Schema validation at the application boundary for versioned payload shapes.
- Consider GIN indexes on frequently queried payload fields if query patterns emerge.
- Future steps can be added with new `onboarding_step_key` enum values and new payload DTOs, without altering the core table structure.
