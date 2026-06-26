# Itinera Implementation Plan

## Goal

Build a correct, resumable, idempotent onboarding vertical slice using Kotlin, React, and PostgreSQL.

The project should demonstrate senior engineering judgment under a constrained delivery window.

## Phase 0 — Repository Bootstrap (DONE)

Deliverables:

* Monorepo structure.
* Backend folder.
* Frontend folder.
* `README.md` skeleton.
* `AGENTS.md`.
* `docs/agents/tasks/`.
* `docs/agents/reports/`.
* `docs/adr/`.
* `AI_LOG.md`.

Success criteria:

* Repo structure is clear.
* Agents understand project goals and constraints.

## Phase 1 — Backend Foundation (DONE)

Deliverables:

* Spring Boot Kotlin app.
* Health endpoint.
* PostgreSQL configuration.
* Flyway migration setup.
* Docker Compose with PostgreSQL.

Success criteria:

* Backend starts locally.
* Flyway migrations run.
* Tests can execute.

## Phase 2 — Persistence Model [DOME]

Deliverables:

* `onboarding_session`.
* `onboarding_step_state`.
* `provider_validation_attempt`.
* `partner_account`.
* PostgreSQL enums.
* Repository layer using `NamedParameterJdbcTemplate`.

Success criteria:

* Session state survives restart.
* Step payloads are stored as JSONB.
* Unique constraints support idempotency.

## Phase 3 — Workflow Domain

Deliverables:

* Workflow state model.
* Static workflow definition.
* Transition validation.
* Allowed actions calculation.
* Typed DTOs for step payloads.

Success criteria:

* Backend is source of truth.
* Invalid transitions are rejected.
* Editing credentials invalidates prior validation.

## Phase 4 — Provider Validation

Deliverables:

* `ProviderValidationPort`.
* `FakeProviderValidationClient`.
* Support for valid, partial, invalid, unavailable, and timeout outcomes.
* Attempt audit table writes.
* Validation result persisted into `onboarding_step_state`.

Success criteria:

* Validation is safe to retry.
* Last validation result is resumable.
* Attempts are auditable.

## Phase 5 — Go Live

Deliverables:

* Transactional go-live service.
* Idempotent creation of `partner_account`.
* Session marked complete.
* Prevent go-live unless validation is valid or partial.

Success criteria:

* Calling go-live twice does not duplicate accounts.
* No half-committed state is possible.

## Phase 6 — REST API Contract

Deliverables:

* Create/resume session endpoint.
* Get session endpoint.
* Submit details endpoint.
* Trigger validation endpoint.
* Go-live endpoint.
* Error responses for invalid transitions.

Success criteria:

* Frontend can drive the full flow using backend state only.
* API returns current step, validation status, payload summary, and allowed actions.

## Phase 7 — Frontend Wizard

Deliverables:

* React wizard with 3 fixed steps.
* `localStorage` session id.
* Resume flow on reload.
* Details form.
* Validation screen with retry.
* Review & go-live screen.

Success criteria:

* Reload resumes from backend state.
* UI clearly surfaces pending, valid, partial, invalid, unavailable.
* User can retry validation safely.

## Phase 8 — Tests

Deliverables:

* Unit tests for workflow transitions.
* Unit tests for Provider result mapping.
* Integration tests for persistence and go-live idempotency.
* Optional frontend smoke test if time allows.

Success criteria:

* Tests run with one command.
* Tests cover the system behaviors called out by the prompt.

## Phase 9 — Documentation Finalization

Deliverables:

* README complete.
* Architecture summary.
* ADRs.
* AI log.
* Future forwards.
* Run/test instructions.

Success criteria:

* Evaluator can clone, run, test, and understand tradeoffs quickly.
