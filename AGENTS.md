# AGENTS.md

Itinera is a resumable partner onboarding platform: Kotlin + React + PostgreSQL vertical slice. Backend owns all workflow state; frontend renders it.

## Project Status

Phase 0 (Bootstrap), Phase 1 (Backend Foundation), and Phase 2 (Persistence Model) are complete. The frontend directory is empty — implementation of Phases 3–9 is pending.

## Running

```bash
docker compose up -d              # PostgreSQL 16 on :5432
cd backend && ./gradlew bootRun   # backend (needs DB)
```

## Testing

```bash
cd backend && ./gradlew test
./gradlew test --tests "com.qualitara.itinera.SomeTest"  # single test
```

## JDK Requirement (Critical)

System JDK must be **21**. Kotlin 2.1.21 compiles to JVM 21 bytecode; running under JDK 25+ causes `IllegalArgumentException: 25.0.2` at startup. Install with `brew install openjdk@21` if needed.

## Architecture

### Workflow State Machine
```
DETAILS → VALIDATION → REVIEW → LIVE
```
- `allowedActions` in session response drives all frontend transitions
- Submitting details is idempotent; changing credentials resets to VALIDATION
- Validation records a new attempt each time; latest replaces current step state
- Go-live is transactional: create-or-reuse partner account + mark live + complete session

### API Contract (Planned)
```
POST   /api/onboarding/sessions
GET    /api/onboarding/sessions/{sessionId}
PUT    /api/onboarding/sessions/{sessionId}/details
POST   /api/onboarding/sessions/{sessionId}/validate
POST   /api/onboarding/sessions/{sessionId}/go-live
```
Session response: `currentStep`, `status`, `details` (apiKeyPresent/apiKeyMasked only — never raw key), `validation`, `allowedActions`.

### Persistence
| Table | Purpose |
|---|---|
| `onboarding_session` | session lifecycle |
| `onboarding_step_state` | per-step JSONB payload; unique on `(session_id, step_key)` |
| `provider_validation_attempt` | validation audit history |
| `partner_account` | final live account; unique on `session_id` |

Use `NamedParameterJdbcTemplate` — not JPA. Migrations in `backend/src/main/resources/db/migration/` (Flyway `V*.sql`).

### Provider Integration
```kotlin
interface ProviderValidationPort {
    fun validate(request: ProviderValidationRequest): ProviderValidationResult
}
```
Outcomes: `VALID`, `PARTIAL`, `INVALID`, `UNAVAILABLE`, `TIMEOUT`. Fake client uses deterministic trigger values on `accountId`.

## Conventions

- **Report completed tasks**: write `docs/agents/reports/<NNN>-<name>.md` + entry in `AI_LOG.md`
- **Never silently expand scope**: document out-of-scope items in `docs/FUTURE_FORWARDS.md` or an ADR
- **API keys never returned** — only `apiKeyPresent` and `apiKeyMasked`
- When uncertain, prefer the simplest implementation that preserves the architecture

## Non-Goals

Do not implement: authentication/login, real third-party Provider integration, dynamic form engine, Kubernetes/CI/production infra, multi-stage Docker builds, visual polish beyond a clear usable wizard.

## Key Docs
- `README.md`, `docs/ARCHITECTURE.md`, `docs/PLAN.md`
- `docs/FUTURE_FORWARDS.md` — deferred work register
- `docs/adr/` — architecture decision records
- `AI_LOG.md` — task/report index