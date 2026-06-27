# ADR 0005 — Keep PostgreSQL as a Persistence Boundary, Not a Business Rule Engine

## Status

Accepted

## Context

PostgreSQL offers stored procedures, triggers, views, generated columns, and business-rule extensions. These could encode workflow transitions (e.g., "a session can only reach LIVE if the latest validation is VALID or PARTIAL") directly in the database.

The alternative is to put all business logic in the Kotlin application and use the database only to store and retrieve state.

## Decision

PostgreSQL is a persistence boundary only.

**PostgreSQL is responsible for:**
- Tables, enums, primary keys, foreign keys, and unique constraints.
- JSONB storage.
- Timestamp defaults via `now()`.
- Index maintenance.

**PostgreSQL is not responsible for:**
- Workflow transition logic.
- Idempotency business rules beyond uniqueness constraints.
- Stored procedures, triggers, views, or computed columns.
- Business validation, cache logic, or observability.

**Repository layer is responsible for:**
- SQL execution and parameter binding.
- Row mapping from JDBC `ResultSet` to Kotlin records.
- JSONB serialization and deserialization via `JsonbPayloadMapper`.

**Service layer is responsible for:**
- Workflow transitions and allowed-action computation.
- Payload validation and business rule enforcement.
- Transaction boundaries (`@Transactional`).
- Idempotency orchestration.
- Provider call orchestration.
- Application logging and observability.

The current fake-Provider validation is synchronous and in-process, so its orchestration uses one application transaction. This does not set the design for a real HTTP Provider: external I/O should occur outside a database transaction, with `PENDING` committed before the call and the final attempt/outcome committed afterward.

## `partner_account` Cascade Decision

`onboarding_step_state` and `provider_validation_attempt` use `ON DELETE CASCADE` on their `session_id` foreign key. Deleting a session cascades to its step states and validation history — these are session-owned lifecycle records with no independent existence.

`partner_account` intentionally does **not** use `ON DELETE CASCADE`.

A partner account represents a live business entity created as the outcome of a completed onboarding session. It is not a lifecycle record owned by the session — it outlives the session conceptually and has independent value. Silently deleting a live partner account because its originating session was deleted would be a destructive operation that belongs in explicit service logic, not a database cascade.

Any deletion of partner accounts must be explicit and service-owned. This design makes accidental deletion much harder.

## Rationale

Business rules evolve much faster than persistence structure. Keeping rules in the Kotlin application provides:

- Reliable unit testing without a database.
- Consistent language, tooling, and debugging environment.
- Simpler schema migrations (structure only, no behavior).
- Clearer ownership — a single layer enforces each concern.

## Tradeoffs

**Benefits:**
- All business rules are co-located with the domain model and testable without PostgreSQL.
- Schema migrations remain focused on structure, not behavior.
- The persistence layer can be swapped or upgraded without touching business logic.

**Costs:**
- Some consistency guarantees require both a DB constraint and an application check.
- A bug in service-layer validation could produce inconsistent state that DB constraints alone would not prevent.
- Does not leverage PostgreSQL's native optimizations for certain rule types.

## Future Forward

- Consider CHECK constraints for enum state transitions if the fixed workflow is long-lived and transitions stabilize.
- If multiple services ever write to this schema, revisit where transition guards should live and whether DB constraints should provide a safety net.
