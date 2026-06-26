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

Review `docs/adr/`.

There appear to be duplicate ADR files using different naming conventions, for example:

```text
ADR0003.md
0003-hybrid-relational-jsonb-step-state.md
```

Choose one convention and keep it consistent.

Preferred convention:

```text
0001-use-spring-boot-flyway-jdbc.md
0002-backend-owned-static-workflow.md
0003-hybrid-relational-jsonb-step-state.md
0004-version-jsonb-payloads-at-application-boundary.md
0005-keep-postgresql-as-persistence-boundary.md
```

Remove or consolidate duplicates.

Do not lose meaningful ADR content. If duplicate files differ, merge the better content into the canonical file before deleting the duplicate.

### 2. Document `partner_account` Cascade Decision

The schema intentionally does not define `ON DELETE CASCADE` on `partner_account.session_id`, unlike step state and validation attempts.

Document this decision explicitly.

Preferred location:

```text
docs/adr/0005-keep-postgresql-as-persistence-boundary.md
```

Add a short section explaining:

* `onboarding_step_state` and `provider_validation_attempt` are session-owned lifecycle records and can cascade.
* `partner_account` represents a live business account created from onboarding.
* Deleting an onboarding session should not silently delete a live partner account.
* Any deletion of partner accounts should be explicit and service-owned.

Do not change the database schema unless the existing implementation contradicts the architecture.

### 3. Optional Small Consistency Fix

If trivial and low risk:

* replace duplicate inline `PGobject` creation in repositories with `JsonbPayloadMapper.toPGobject()`.

Only do this if it does not cause refactoring churn.

## Out of Scope

Do not:

* add new workflow services
* add new REST endpoints
* change the schema
* add new migrations
* implement Provider validation
* change repository semantics
* add frontend code
* introduce logging policy changes

## Validation

Run:

```bash
cd backend
./gradlew test
```

Expected:

```text
BUILD SUCCESSFUL
```

## Documentation

Create:

```text
docs/agents/tasks/003c-persistence-cleanup-before-phase-3.md
docs/agents/reports/003c-persistence-cleanup-before-phase-3.md
```

Update:

```text
AI_LOG.md
```

## Report Requirements

Use the standard report structure:

* Summary
* Deliverables
* Validation
* Tests
* Engineering Notes
* Tradeoffs
* Follow-ups

The report should describe the capability:

```text
Persistence documentation and ADR baseline stabilized.
```

## Success Criteria

* ADR files have a single clear naming convention.
* Duplicate ADR content is resolved.
* `partner_account` cascade decision is documented.
* Tests still pass.
* Phase 2 can be marked complete.
