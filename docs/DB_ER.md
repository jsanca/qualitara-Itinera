# Database Schema

The persistence model is built around a central `onboarding_session` that tracks where a partner is in the onboarding flow and whether they have gone live. Each session owns a set of `onboarding_step_state` records — one per wizard step — that store the step's current status and its submitted payload as JSONB, allowing the flow to be resumed at any point after a page reload or server restart.

Validation attempts are recorded separately in `provider_validation_attempt`, giving a full audit trail of every call made to the external Provider, including retries and failure outcomes. Once a partner completes the flow, a `partner_account` row is created in the same transaction that marks the session complete, ensuring no half-committed state is possible.

The raw Provider API key is not stored in any table or JSONB payload. Details state contains a masked display value and SHA-256 credential fingerprint; validation attempts contain only a request fingerprint.

```mermaid
erDiagram
    onboarding_session {
        uuid id PK
        onboarding_step_key current_step
        onboarding_session_status status
        timestamptz created_at
        timestamptz updated_at
        timestamptz completed_at
    }

    onboarding_step_state {
        uuid id PK
        uuid session_id FK
        onboarding_step_key step_key
        onboarding_step_status status
        jsonb payload
        integer payload_version
        timestamptz created_at
        timestamptz updated_at
        timestamptz completed_at
    }

    provider_validation_attempt {
        uuid id PK
        uuid session_id FK
        integer attempt_number
        text account_id
        text request_fingerprint
        provider_validation_outcome outcome
        jsonb response_payload
        text error_message
        timestamptz started_at
        timestamptz completed_at
    }

    partner_account {
        uuid id PK
        uuid session_id FK
        text company_name
        partner_account_status status
        timestamptz went_live_at
        timestamptz created_at
    }

    onboarding_session ||--o{ onboarding_step_state : "has steps"
    onboarding_session ||--o{ provider_validation_attempt : "has attempts"
    onboarding_session ||--o| partner_account : "becomes"
```

## Enums

| Type | Values |
|---|---|
| `onboarding_step_key` | `DETAILS`, `VALIDATION`, `REVIEW`, `COMPLETE` |
| `onboarding_session_status` | `DRAFT`, `LIVE` |
| `onboarding_step_status` | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED` |
| `provider_validation_outcome` | `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, `TIMEOUT` |
| `partner_account_status` | `LIVE` |

## Constraints and Lifecycle

| Constraint | Purpose |
|---|---|
| `UNIQUE (onboarding_step_state.session_id, step_key)` | One latest resumable payload per step; supports upsert. |
| `UNIQUE (provider_validation_attempt.session_id, attempt_number)` | Stable ordered audit history for each session. |
| `UNIQUE (partner_account.session_id)` | At most one live account from an onboarding session. |
| Step/attempt foreign keys with `ON DELETE CASCADE` | Step state and audit attempts are session-owned lifecycle records. |
| Partner-account foreign key without cascade | A live business account cannot be deleted implicitly with its onboarding session. |

`payload_version` is duplicated alongside the embedded JSON `version`: JSON remains self-describing while the relational column supports compatibility checks and future migrations.

The schema is defined by [`backend/src/main/resources/db/migration/V1__initial_backend_foundation.sql`](../backend/src/main/resources/db/migration/V1__initial_backend_foundation.sql). Persistence rationale is recorded in ADRs [0003](adr/0003-hybrid-relational-jsonb-step-state.md), [0004](adr/0004-version-jsonb-payloads-at-application-boundary.md), and [0005](adr/0005-keep-postgresql-as-persistence-boundary.md).
