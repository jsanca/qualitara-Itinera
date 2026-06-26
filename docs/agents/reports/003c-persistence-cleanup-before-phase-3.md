# Report 003C — Persistence Cleanup Before Phase 3

## Summary

Persistence documentation and ADR baseline stabilized. The ADR directory now has a single consistent naming convention, duplicate content has been merged (preserving the richer material from each source), the `partner_account` cascade decision is explicitly documented, and inline `PGobject` construction is consolidated through `JsonbPayloadMapper`. Phase 2 is complete.

## Deliverables

- **ADR directory consolidated** — 10 files (5 old format + 5 new format) reduced to 5 canonical files under the `000N-name.md` convention.
- **`0001-use-spring-boot-flyway-jdbc.md`** — created from `ADR0001.md` (no prior canonical file existed).
- **`0002-backend-owned-static-workflow.md`** — created from `ADR0002.md` (no prior canonical file existed).
- **`0003-hybrid-relational-jsonb-step-state.md`** — merged: adopted cleaner context structure from 0003; added table schemas and type safety strategy from ADR0003.
- **`0004-version-jsonb-payloads-at-application-boundary.md`** — merged: kept 0004 decision structure; added rationale section and future evolution detail from ADR0004.
- **`0005-keep-postgresql-as-persistence-boundary.md`** — merged: kept 0005 context and decision; added ADR0005's full responsibility breakdown (PostgreSQL / Repository / Service); added `partner_account` cascade section.
- **`ADR0001.md` – `ADR0005.md` deleted.**
- **`JsonbPayloadMapper.wrapJsonString()`** and **`wrapJsonStringOrNull()`** — new methods wrapping a pre-serialized JSON string in a JSONB `PGobject` without re-encoding.
- **`OnboardingStepStateRepository`** and **`ProviderValidationAttemptRepository`** — inline `PGobject().apply { ... }` replaced with mapper calls; `PGobject` import removed from both repositories.

## Validation

```
./gradlew test  →  BUILD SUCCESSFUL, 9 tests passed
```

No behavior changed. Tests pass on the same schema and data.

## Tests

No new tests added. The 9 existing tests exercise the two modified repositories (`OnboardingStepStateRepository` via upsert and JSONB roundtrip; `ProviderValidationAttemptRepository` via insert and ordering). All pass.

## Engineering Notes

**`wrapJsonString` vs `toPGobject`.** The existing `toPGobject(Any)` method serializes a Kotlin object to JSON and wraps it. Using it with a pre-serialized JSON string would double-encode the string (the `String` itself would become a JSON string literal). A distinct `wrapJsonString(String)` method makes this boundary explicit and prevents the mistake from compiling silently.

**ADR content strategy.** The `ADR000N.md` files (created during earlier review passes) generally had richer rationale and responsibility breakdowns. The `000N-name.md` files (created during Task 003) had cleaner comparative context structure. Each merged file was written to combine both: structured framing from the new format, detailed rationale from the old format. No content was lost.

**`partner_account` cascade is intentional.** The absence of `ON DELETE CASCADE` on `partner_account.session_id` was flagged as a potential oversight in Report 003. It is confirmed intentional: a partner account has independent business identity and must not be deleted as a side-effect of session cleanup. The cascade section in ADR 0005 makes this permanent engineering record so future contributors do not add CASCADE as a cleanup convenience.

## Tradeoffs

No material tradeoffs in this cleanup task. The `wrapJsonString` addition adds one method to `JsonbPayloadMapper`; it could have been left inline, but the name makes the intent explicit and prevents accidental double-encoding.

## Follow-ups

Phase 2 is complete. Next: **Phase 3 — Onboarding Session API** from `docs/PLAN.md`.

This covers the session service, workflow model, `allowedActions` computation, and the first REST endpoints (`POST /api/onboarding/sessions`, `GET /sessions/{id}`). The schema, repositories, and ADRs are now stable foundations for that work.
