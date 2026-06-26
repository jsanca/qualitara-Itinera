# ADR 0004 — Version JSONB Payloads at the Application Boundary

## Status

Accepted

## Context

Step payloads are stored as JSONB. Without versioning, there is no way to detect stale payloads, deserialize historical records correctly after a schema change, or migrate old payloads in a controlled way.

Each onboarding step has a different payload structure (Details, Validation, Review) that is expected to evolve. PostgreSQL does not enforce the internal structure of JSONB documents; without additional safeguards, malformed or incompatible payloads could be persisted and become unreadable under newer application versions.

Two approaches were considered:

1. **No versioning** — deserialize payloads by current DTO shape; rely on backward-compatible JSON field additions only.
2. **Explicit payload version** — embed a `version` field in every payload; mirror it as a queryable relational column.

## Decision

Every step payload implements the `VersionedPayload` interface:

```kotlin
interface VersionedPayload {
    val version: Int
}
```

The version is embedded in the JSON payload and also stored in the `payload_version` column on `onboarding_step_state`. This is intentionally redundant:

- The JSON is self-describing and portable.
- The column supports future SQL queries and bulk migrations (`WHERE payload_version < 2`) without parsing JSONB.

Validation of payload structure and version compatibility is the application's responsibility — not PostgreSQL's.

## Rationale

Keeping payload validation inside the application:

- Keeps business rules in Kotlin rather than PostgreSQL.
- Allows payload evolution through explicit adapters or migration strategies.
- Keeps version compatibility auditable and testable.
- Decouples persistence format from deserialization logic.

## Consequences

**Benefits:**
- Payloads can be migrated without guessing what shape they were written under.
- The `payload_version` column supports targeted bulk migrations.
- Clear extension point for JSON Schema validation per version.

**Tradeoffs:**
- Every payload class must carry a `version` field — minor overhead.
- The redundant column must be kept consistent with the embedded JSON at write time; enforced at the repository layer.
- Payload compatibility across versions must be managed explicitly in the service layer.

## Future Forward

- Add per-version JSON Schema validation via `JsonbPayloadMapper`.
- Write a migration utility that upgrades old payloads in bulk using `payload_version` as a filter.
- Introduce a payload version deprecation policy once more than one version exists in production.
