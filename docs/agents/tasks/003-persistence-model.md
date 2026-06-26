# Task 003 — Persistence Model

## Context

Project: Itinera

Phase 2 establishes the persistent domain model for the onboarding workflow.

The backend already starts, connects to PostgreSQL, runs Flyway, exposes health, and can execute tests. This task replaces the temporary foundation migration with the real onboarding persistence schema and adds the repository layer.

Treat PostgreSQL as a persistence boundary, not as a business rule engine.

## Goal

Implement the PostgreSQL schema and repository layer for resumable onboarding state.

The system should be able to persist and retrieve:

* onboarding sessions
* per-step state payloads
* Provider validation attempts
* partner accounts

No onboarding business workflow should be implemented yet.

## Scope

Create:

* PostgreSQL enums
* Flyway migration for the real schema
* Kotlin persistence/domain records
* repository classes using `NamedParameterJdbcTemplate`
* JSONB payload serialization/deserialization using Jackson
* repository tests proving persistence and idempotency-supporting constraints

## Migration

Overwrite the existing temporary migration:

```text
backend/src/main/resources/db/migration/V1__initial_backend_foundation.sql
```

Replace its contents with the real schema.

Do not keep `app_schema_version_marker`.

Do not create `V2` for this task.

## Database Responsibilities

PostgreSQL should handle:

* tables
* enums
* primary keys
* foreign keys
* unique constraints
* JSONB storage
* timestamp defaults where useful

PostgreSQL should not handle:

* business validation
* workflow transition logic
* idempotency business rules
* cache behavior
* observability
* application logging
* service-level transactions
* stored procedures
* triggers
* views

Keep the database simple: store and retrieve state.

## PostgreSQL Enums

Create these enums:

```sql
create type onboarding_step_key as enum ('DETAILS', 'VALIDATION', 'REVIEW', 'COMPLETE');
create type onboarding_session_status as enum ('DRAFT', 'LIVE');
create type onboarding_step_status as enum ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED');
create type provider_validation_outcome as enum ('VALID', 'PARTIAL', 'INVALID', 'UNAVAILABLE', 'TIMEOUT');
create type partner_account_status as enum ('LIVE');
```

## Tables

See task brief for full `CREATE TABLE` statements.

## Kotlin Model

Enums: `OnboardingStepKey`, `OnboardingSessionStatus`, `OnboardingStepStatus`, `ProviderValidationOutcome`, `PartnerAccountStatus`

Records: `OnboardingSessionRecord`, `OnboardingStepStateRecord`, `ProviderValidationAttemptRecord`, `PartnerAccountRecord`

## JSONB Payload Handling

`JsonbPayloadMapper` — serialize/deserialize; extract raw JSONB string from `ResultSet`.

`VersionedPayload` interface with `val version: Int`.

## Repository Layer

`OnboardingSessionRepository`, `OnboardingStepStateRepository`, `ProviderValidationAttemptRepository`, `PartnerAccountRepository`

Use `NamedParameterJdbcTemplate`. No JPA. No workflow logic.

## Tests

8 repository integration tests covering: create/retrieve session, upsert replaces, JSONB roundtrip, attempt ordering, partner account CRUD, and unique constraint enforcement for step state, attempt number, and partner account session_id.

## Documentation Updates

* `AI_LOG.md`
* `backend/README.md` if commands changed
* `docs/FUTURE_FORWARDS.md`
* `docs/agents/reports/003-persistence-model.md`
* `docs/adr/0003-hybrid-relational-jsonb-step-state.md`
* `docs/adr/0004-version-jsonb-payloads-at-application-boundary.md`
* `docs/adr/0005-keep-postgresql-as-persistence-boundary.md`

## Validation Commands

```bash
docker compose down -v && docker compose up -d postgres
cd backend
./gradlew test
```

## Constraints

Do not implement onboarding workflow services, REST APIs, Provider validation, go-live transaction, JPA, stored procedures, triggers, views, or frontend code.
