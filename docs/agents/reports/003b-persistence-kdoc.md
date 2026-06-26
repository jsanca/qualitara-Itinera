# Report: Persistence KDoc and Internal API Documentation

## Summary

Added KDoc to all persistence layer types in `backend/src/main/kotlin/com/qualitara/itinera/persistence/`, covering model records, repositories, JSON helpers, and three subpackages. The documentation clarifies ownership boundaries — what each type owns vs. what it intentionally does not — without changing behavior.

## Deliverables

### Model Records (4 files edited)

**`Enums.kt`**
Added class-level KDoc to each enum explaining purpose and, where non-obvious, the semantic role of values (e.g., `BLOCKED` means a step cannot proceed due to a prior failure).

**`OnboardingSessionRecord.kt`**
Documents that the record is a persistence representation only — no business logic, no transition ownership. Includes descriptions for all fields.

**`OnboardingStepStateRecord.kt`**
Clarifies that payload versioning is a structural signal only, not a schema migration mechanism. Calls out that callers are responsible for payload shape interpretation.

**`ProviderValidationAttemptRecord.kt`**
Notes that attempt numbering derives solely from `ProviderValidationAttemptRepository.nextAttemptNumber`. Documents that this record does not own Provider call logic or retry policy.

### Repositories (4 files edited)

Each repository received class-level KDoc covering:
- What it owns (SQL execution, row mapping, persistence shape)
- What it does not own (workflow transitions, business validation, transaction orchestration, cache, observability)
- Any notable persistence semantics (e.g., upsert idempotency via `ON CONFLICT`, unique-constraint anchor for idempotent go-live)

### JSON Helpers (2 files edited)

**`JsonbPayloadMapper.kt`**
States the three things it does not do: validate business meaning, enforce schema compatibility, or interpret payload versions.

**`VersionedPayload.kt`**
Explains the interface is a persistence-level marker only. Version is written at payload creation time; callers decide how to interpret it on read.

### Package-Level Documentation (3 new files)

Created `package-info.kt` in `persistence/model/`, `persistence/repository/`, and `persistence/json/`. Each states:
- The package's responsibility
- A clear boundary list of what the package does NOT own

The `repository/package-info.kt` also explicitly notes the transaction boundary convention: repositories execute within transactions opened by their callers.

## Validation

All Kotlin source compiled successfully. No behavioral changes.

## Tests

```
cd backend && ./gradlew test
```
9 tests — all pass (requires PostgreSQL via `docker compose up -d`).

## Engineering Notes

- `@file:Suppress("PACKAGE")` is used in `package-info.kt` files to suppress Kotlin's "redundant package directive" warning, which is the idiomatic pattern for package documentation in Kotlin.
- KDoc was kept concise — class-level over every-member documentation — per the task guidance.
- No new types were introduced; all documentation is additive.
- The `package-info.kt` convention is well-established in Kotlin/JVM codebases and integrates cleanly with Dokka.

## Tradeoffs

- **Package docs vs. file docs**: Class-level KDoc covers individual type contracts; package-level docs (`package-info.kt`) set the layer-wide ownership narrative. Both are used because the persistence layer has two distinct audiences: callers of a repository (who need to know what the repository does not own) and maintainers understanding the layer's intent.
- **`@file:Suppress("PACKAGE")`**: Some teams prefer a README within the package instead. `package-info.kt` is more idiomatic for Kotlin libraries but requires the suppress annotation to avoid IDE warnings.

## Follow-ups

- If payload schema evolution becomes necessary, the `VersionedPayload` documentation should be expanded to describe the migration strategy at that time.
- `package-info.kt` files can be picked up by Dokka to generate API documentation; no additional configuration is needed given the existing setup.
