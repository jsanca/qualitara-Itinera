# AGENTS.md

This file provides guidance to OpenCode when working in this repository.

## Project: Itinera

Itinera is a resumable partner onboarding platform implemented as a Kotlin + React + PostgreSQL vertical slice. Treat it as a real system — prefer correctness, tradeoffs, and testability over feature breadth.

## Current State

**Phase 0 (Repository Bootstrap)** is complete. The repo has Docker Compose (PostgreSQL 16), a Spring Boot backend skeleton with Flyway, and documentation conventions. The frontend directory exists but contains no code yet. All architecture below represents the design intent — most of it is not yet implemented.

## Command Quick Reference

```bash
docker compose up -d              # start PostgreSQL 16 on :5432
```

```bash
cd backend
./gradlew bootRun                 # start backend (needs PostgreSQL running)
./gradlew test                    # all tests
./gradlew test --tests "com.qualitara.itinera.SomeTest"  # single test
```

## JDK Requirement (Critical)

The system JDK must be **21**, not a newer version. Kotlin 1.9.25 used by this project crashes when parsing Java version strings from JDK 25+. The build file declares `JavaLanguageVersion.of(21)` — install JDK 21 if you don't have it (e.g., `brew install openjdk@21`). `gradlew` will fail with `IllegalArgumentException: 25.0.2` if this is wrong.

## Architecture (Design Intent)

### Workflow State Machine

```
DETAILS → VALIDATION → REVIEW → LIVE
```

- Backend owns all workflow state; the frontend renders it.
- `allowedActions` in the session response drives all frontend transitions — the frontend never invents state changes.
- Submitting details is idempotent; changing credentials after a valid/partial result resets to VALIDATION.
- Validation records a new attempt each time; latest result replaces current step state.
- Go-live is transactional: create-or-reuse partner account + mark live + complete session.

### API Contract (Planned)

```
POST   /api/onboarding/sessions
GET    /api/onboarding/sessions/{sessionId}
PUT    /api/onboarding/sessions/{sessionId}/details
POST   /api/onboarding/sessions/{sessionId}/validate
POST   /api/onboarding/sessions/{sessionId}/go-live
```

Session response includes `currentStep`, `status`, `details` (with `apiKeyPresent`/`apiKeyMasked`, never raw key), `validation`, and `allowedActions`.

### Persistence Model (Planned)

| Table | Purpose |
|---|---|
| `onboarding_session` | session lifecycle |
| `onboarding_step_state` | per-step payload (JSONB); unique on `(session_id, step_key)` |
| `provider_validation_attempt` | validation audit history |
| `partner_account` | final live account; unique on `session_id` |

Use `NamedParameterJdbcTemplate` — not JPA. Migrations live in `backend/src/main/resources/db/migration/` (Flyway `V*.sql`). Step payloads are typed Kotlin DTOs (`DetailsPayload`, `ValidationPayload`, `ReviewPayload`).

### Provider Integration

```kotlin
interface ProviderValidationPort {
    fun validate(request: ProviderValidationRequest): ProviderValidationResult
}
```

Outcomes: `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, `TIMEOUT`. The fake client uses deterministic trigger values on `accountId`.

## Conventions

- **Report every completed task**: write `docs/agents/reports/<NNN>-<name>.md` and an entry in `AI_LOG.md`. Reports explain *why*, not just *what*.
- **Never silently expand scope**: document out-of-scope items in `docs/FUTURE_FORWARDS.md` or an ADR.
- **When uncertain, prefer the simplest implementation that preserves the architecture.**
- **API keys are never returned in responses** — only `apiKeyPresent` and `apiKeyMasked`.

## Non-Goals

Do not implement: authentication/login, real third-party Provider integration, dynamic form engine, Kubernetes/CI/production infra, multi-stage Docker builds, visual polish beyond a clear usable wizard.

## Key Docs

- `README.md`, `docs/ARCHITECTURE.md`, `docs/PLAN.md`
- `docs/FUTURE_FORWARDS.md` — deferred work register
- `docs/adr/` — architecture decision records
- `AI_LOG.md` — index of tasks and reports
