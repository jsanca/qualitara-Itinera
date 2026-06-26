# Task 003C — Persistence Cleanup Before Phase 3

## Context

Project: Itinera

Phase 2 implemented the persistence model and repository layer. Deep review found no critical issues, and follow-up repository tests were added in Task 003A.

Before marking Phase 2 complete, perform a small cleanup pass to remove documentation ambiguity and capture one schema decision explicitly.

Do not change business behavior.

## Goal

Close the remaining Phase 2 review items that should be resolved before Phase 3 begins.

## Scope

### 1. ADR Duplicate Cleanup

Consolidate ADR files to a single naming convention:

```text
0001-use-spring-boot-flyway-jdbc.md
0002-backend-owned-static-workflow.md
0003-hybrid-relational-jsonb-step-state.md
0004-version-jsonb-payloads-at-application-boundary.md
0005-keep-postgresql-as-persistence-boundary.md
```

Remove duplicate `ADR000N.md` files. Merge richer content from duplicates before deleting.

### 2. Document `partner_account` Cascade Decision

Add a section to `0005-keep-postgresql-as-persistence-boundary.md` explaining:

- `onboarding_step_state` and `provider_validation_attempt` cascade on session delete (session-owned lifecycle records).
- `partner_account` does not cascade — it represents a live business entity that outlives the session.
- Deletion of partner accounts must be explicit and service-owned.

### 3. Replace Inline PGobject Creation in Repositories

Replace duplicate `PGobject().apply { type = "jsonb"; value = ... }` expressions in repositories with `JsonbPayloadMapper.wrapJsonString()` and `wrapJsonStringOrNull()`.

## Validation

```bash
cd backend
./gradlew test
```

Expected: `BUILD SUCCESSFUL`

## Documentation

Create:

```text
docs/agents/tasks/003c-persistence-cleanup-before-phase-3.md
docs/agents/reports/003c-persistence-cleanup-before-phase-3.md
```

Update: `AI_LOG.md`
