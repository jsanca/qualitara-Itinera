# Itinera Backend

Kotlin/Spring Boot API for the backend-owned Itinera onboarding workflow.

## Requirements

- JDK 21
- PostgreSQL 16, normally started through the root Docker Compose file

## Run

From the repository root, start PostgreSQL:

```bash
docker compose up -d postgres
```

Then run from this directory:

```bash
./gradlew bootRun
```

The API listens on `http://localhost:8080`. Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Test

Tests include pure domain tests and PostgreSQL-backed Spring integration tests, so PostgreSQL must be running.

```bash
./gradlew test
```

Run one class:

```bash
./gradlew test --tests "com.qualitara.itinera.api.OnboardingApiIntegrationTest"
```

## Structure

- `api/` — REST controller, request/response DTOs, error mapping, application orchestration
- `workflow/` — pure state transitions and allowed-action policy
- `provider/` — validation port, deterministic fake, attempt orchestration
- `golive/` — transactional and idempotent completion service
- `internal/persistence/` — JDBC repositories, stored records, JSONB mapping
- `resources/db/migration/` — Flyway schema

The Provider fake is driven by exact `accountId` values: `valid`, `partial`, `invalid`, `unavailable`, and `timeout`. Raw API keys are never persisted or returned.

See the root [README](../README.md), [architecture](../docs/ARCHITECTURE.md), and [API contract](../docs/API_CONTRACT.md).
