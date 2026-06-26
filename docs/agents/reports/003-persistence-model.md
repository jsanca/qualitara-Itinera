# Report 003 — Persistence Model

## Summary

The onboarding domain now has a persistent foundation. All four core tables exist in PostgreSQL with the right types, constraints, and JSONB columns. The repository layer can persist and retrieve every record type. Unique constraints enforce the structural conditions that idempotency and audit ordering will depend on in later slices.

## Deliverables

- **Flyway migration `V1`** — replaced the foundation placeholder with the real schema: five PostgreSQL enums and four tables (`onboarding_session`, `onboarding_step_state`, `provider_validation_attempt`, `partner_account`). The temporary `app_schema_version_marker` table was removed.
- **Kotlin enums** — `OnboardingStepKey`, `OnboardingSessionStatus`, `OnboardingStepStatus`, `ProviderValidationOutcome`, `PartnerAccountStatus` in `persistence/model/Enums.kt`.
- **Persistence records** — `OnboardingSessionRecord`, `OnboardingStepStateRecord`, `ProviderValidationAttemptRecord`, `PartnerAccountRecord` as data classes.
- **`VersionedPayload` interface** — `val version: Int` marker for all JSONB payloads.
- **`JsonbPayloadMapper`** — Jackson-backed component that serializes to `PGobject(jsonb)` for writes and extracts raw JSON strings from `ResultSet` for reads.
- **Four repositories** using `NamedParameterJdbcTemplate`: insert, find, update, upsert as required per table.
- **8 repository integration tests** covering all required behaviors and constraint verification.
- **3 ADRs** documenting the hybrid relational/JSONB model, payload versioning, and the persistence boundary principle.
- **PostgreSQL JDBC driver** promoted from `runtimeOnly` to `implementation` scope so `PGobject` is available at compile time.

## Validation

```
docker compose down -v && docker compose up -d postgres  →  fresh schema applied
./gradlew test                                           →  BUILD SUCCESSFUL, 9 tests passed
```

Flyway log on test run:
```
Successfully validated 1 migration
Migrating schema "public" to version 1 — initial backend foundation
Successfully applied 1 migration
```

## Tests

9 tests pass:

| Test | What it proves |
|------|---------------|
| `contextLoads` | Spring context and DB wire-up |
| `create and retrieve session` | Session insert + findById roundtrip |
| `upsert step state replaces existing for same key` | ON CONFLICT DO UPDATE replaces payload; one row per `(session_id, step_key)` |
| `store and retrieve jsonb payload with version` | Full Jackson serialization roundtrip through JSONB column |
| `insert and list validation attempts in ascending order` | ORDER BY on `attempt_number` is correct regardless of insert order |
| `insert and retrieve partner account` | findBySessionId and findById both work |
| `step state upsert enforces idempotency — two writes produce one row` | Unique constraint drives idempotent upsert |
| `validation attempt unique constraint rejects duplicate attempt number` | `(session_id, attempt_number)` unique constraint |
| `partner account session id must be unique` | `session_id` unique constraint on `partner_account` |

## Engineering Notes

**`runtimeOnly` vs `implementation` for PostgreSQL JDBC.** Spring Boot's default starter configuration puts the PostgreSQL driver as `runtimeOnly`. Repositories that use `PGobject` (from the driver) directly reference it at compile time. Changed to `implementation` to resolve compilation errors without any runtime change in behavior.

**`partner_account` has no `ON DELETE CASCADE`.** The task migration spec does not include CASCADE on `partner_account.session_id`. This caused all cleanup tests to fail initially: `DELETE FROM onboarding_session` was rejected by the FK when a `partner_account` row still referenced the session. Fixed by using an explicit delete-children-first order in `@AfterEach`. The omission of CASCADE on `partner_account` may be intentional — a live partner account is semantically different from a step state record and may warrant explicit deletion rather than silent cascade. Noted for human review.

**Constraint violation test isolation.** After a `DataIntegrityViolationException`, PostgreSQL marks the connection as ABORTED. Tests that assert constraint violations (`assertThrows`) are structured to perform no further DB operations after the exception, allowing HikariCP to cleanly return the connection to the pool and roll it back.

## Tradeoffs

**No `ON DELETE CASCADE` on `partner_account`.** The other three child tables cascade on session delete; `partner_account` does not. This is consistent with the task specification but creates an asymmetry. A future team decision: if cascading semantics are desired for partner account cleanup, add CASCADE to a future migration rather than patching V1 (Flyway history should not be rewritten once applied to real environments).

**Raw JSON string in persistence records.** `OnboardingStepStateRecord.payloadJson` and `ProviderValidationAttemptRecord.responsePayloadJson` are `String` rather than a typed payload. This keeps the repository layer agnostic of specific payload DTOs. The service layer will use `JsonbPayloadMapper.fromJsonString<T>()` to deserialize to a typed DTO when needed.

## Follow-ups

Next: **Phase 3 — Onboarding Session API** from `docs/PLAN.md`.

This covers the REST endpoints (`POST /api/onboarding/sessions`, `GET /sessions/{id}`), the session service, and the workflow model that determines `currentStep` and `allowedActions`. The schema and repositories built in this slice are the direct foundation for that work.
