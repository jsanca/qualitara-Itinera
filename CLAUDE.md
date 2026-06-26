# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project: Itinera

Itinera is a resumable partner onboarding platform implemented as a production-quality vertical slice.

Although this repository is being built under a constrained evaluation window, treat it as a real system. Prefer correctness, explicit tradeoffs, testability, and maintainability over feature breadth.

## Commands

**Start infrastructure:**
```bash
docker compose up -d
```

**Backend (Kotlin + Spring Boot):**
```bash
cd backend
./gradlew bootRun          # run
./gradlew test             # all tests
./gradlew test --tests "com.qualitara.itinera.SomeTest"   # single test class
```

**Frontend (React + TypeScript):**
```bash
cd frontend
npm install
npm run dev                # dev server
npm test                   # tests
```

## Product Goal

A partner company self-onboards by:

1. Entering company and Provider credentials.
2. Validating the Provider integration.
3. Reviewing discovered Provider items and going live.

The flow must be resumable. The backend is the source of truth for session state, current step, validation result, and allowed actions.

## Architecture

### System Shape

```
React Frontend
      |  HTTP/JSON
      v
Kotlin Spring Boot API
      |                   |
      v                   v
ProviderValidationPort  PostgreSQL
      |
      v
FakeProviderValidationClient (in-process)
```

### Workflow State Machine

```
DETAILS → VALIDATION → REVIEW → LIVE
```

- Submitting details is idempotent; re-submitting replaces the payload. Changing credentials after a valid/partial result resets to VALIDATION.
- Validation records a new attempt each time; the latest result replaces current step state.
- Go-live is transactional: create-or-reuse partner account + mark live + mark session complete.
- `allowedActions` in the session response drives all frontend transitions — the frontend never invents state changes.

### Key Port

```kotlin
interface ProviderValidationPort {
    fun validate(request: ProviderValidationRequest): ProviderValidationResult
}
```

Outcomes: `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, `TIMEOUT`. The `FakeProviderValidationClient` uses deterministic trigger values on `accountId` to exercise each outcome.

### API Contract

```
POST   /api/onboarding/sessions
GET    /api/onboarding/sessions/{sessionId}
PUT    /api/onboarding/sessions/{sessionId}/details
POST   /api/onboarding/sessions/{sessionId}/validate
POST   /api/onboarding/sessions/{sessionId}/go-live
```

Session response includes `currentStep`, `status`, `details` (with `apiKeyPresent`/`apiKeyMasked`, never the raw key), `validation`, and `allowedActions`.

### Persistence Model

Hybrid relational + JSONB:

| Table | Purpose |
|---|---|
| `onboarding_session` | session lifecycle (step, status, timestamps) |
| `onboarding_step_state` | per-step resumable payload (JSONB); unique on `(session_id, step_key)` |
| `provider_validation_attempt` | validation audit history |
| `partner_account` | final live account; unique on `session_id` |

Step payloads are typed Kotlin DTOs (`DetailsPayload`, `ValidationPayload`, `ReviewPayload`). The service layer validates before persisting. Use `NamedParameterJdbcTemplate` — not JPA.

Migrations live in `backend/src/main/resources/db/migration/` (Flyway `V*.sql`).

### Frontend

Stores only `sessionId` in `localStorage`. On load, fetches session and renders based on `currentStep` and `allowedActions`. Never stores the API key after submission or calls the Provider directly.

## Core Engineering Principles

- Backend owns workflow state; frontend renders it.
- Provider integration sits behind a port.
- Go-live is idempotent and transactional.
- JSONB for step payloads; Kotlin DTOs enforce type safety at the application boundary.
- Tests focus on: state transitions, validation outcomes, idempotency, persistence.

## Explicit Non-Goals

Do not implement: authentication/login, real third-party Provider integration, dynamic form engine, Kubernetes/CI/production infra or multi-stage Docker builds, visual polish beyond a clear usable wizard, AI crawling or advanced automation.

## AI Work Rules

AI may generate code, but humans own the design. Agents must not silently expand scope — document out-of-scope items in `docs/FUTURE_FORWARDS.md` or an ADR instead of implementing them. When uncertain, prefer the simplest implementation that preserves the architecture.

## Documentation to Keep Updated

- `README.md`
- `docs/ARCHITECTURE.md`
- `AI_LOG.md`
- `docs/adr/`
- `docs/FUTURE_FORWARDS.md`
- `docs/agents/tasks/` and `docs/agents/reports/`

## Engineering Reports

Every completed task must produce a report at `docs/agents/reports/<NNN>-<name>.md` and an entry in `AI_LOG.md`. No additional prompting is required — this is part of the default workflow.

Reports are engineering documents, not implementation logs. They explain *why* decisions were made, not just *what* was built. Use this structure:

```
## Summary        — engineering outcome delivered, framed as value
## Deliverables   — major artifacts produced (not every file)
## Validation     — how deliverables were verified (runtime, static, manual)
## Tests          — automated tests run; if none, explain why
## Engineering Notes — assumptions, decisions, observations, reasoning
## Tradeoffs      — intentional scope or architecture tradeoffs
## Follow-ups     — next planned slice from docs/PLAN.md by name
```

## AI Log

`AI_LOG.md` is an index into the engineering process — task definitions, implementation reports, and review iterations. It is not a conversation transcript or prompt dump. The artifacts themselves tell the story.
