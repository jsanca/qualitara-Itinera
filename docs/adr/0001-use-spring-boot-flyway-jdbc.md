# ADR 0001 — Use Spring Boot, Flyway, and JDBC for the Backend Foundation

## Status

Accepted

## Context

Itinera needs a Kotlin backend exposing a small REST API, persisting onboarding state in PostgreSQL, and running database migrations reliably.

The submitted slice is intentionally small, but the backend must still show production-oriented engineering judgment: explicit persistence, reliable migrations, testability, and clear transactional boundaries.

## Decision

Use:

- Kotlin
- Spring Boot
- Flyway
- PostgreSQL
- `NamedParameterJdbcTemplate`
- Spring transaction management

Do not use JPA for this slice.

## Rationale

Spring Boot provides a fast, conventional backend foundation with strong support for REST APIs, configuration, health checks, dependency injection, database access, transactions, and testing.

Flyway keeps the database schema explicit through versioned SQL migrations. There is no hidden DDL generation or runtime schema drift.

`NamedParameterJdbcTemplate` keeps persistence simple and transparent. The domain model is small, JSONB is central to the persistence strategy, and explicit SQL makes transactional behavior easy to inspect and debug.

JPA is intentionally avoided for this slice to sidestep Kotlin-specific friction (proxies, open classes, lazy loading, nullability mismatches) and to keep the persistence layer legible.

## Tradeoffs

**Benefits:**
- Fast delivery under a constrained timebox.
- Easy local setup; no ORM configuration overhead.
- Clear SQL and database behavior; no hidden queries.
- Straightforward Flyway migrations with predictable schema history.
- Simple transaction handling for the go-live operation.

**Costs:**
- More manual row mapping code than JPA.
- No automatic change tracking.
- More SQL written by hand.

## Consequences

Repository classes own SQL explicitly. The service layer owns transaction boundaries. The onboarding schema is expressed through Flyway migrations only — no runtime DDL.

Future richer persistence needs may introduce query builders or other helpers, but JPA should be re-evaluated rather than assumed.
