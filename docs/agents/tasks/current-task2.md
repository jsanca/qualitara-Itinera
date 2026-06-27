# Task 011 — Documentation Finalization

## Goal

Finalize documentation so an evaluator can clone, run, test, and understand Itinera quickly.

## Scope

Review and update:

* `README.md`
* `docs/ARCHITECTURE.md`
* `docs/API_CONTRACT.md`
* `docs/DB_ER.md`
* `docs/FUTURE_FORWARDS.md`
* `docs/PLAN.md`
* `docs/adr/`
* `AI_LOG.md`
* backend/frontend README files

## README Must Include

* What Itinera is.
* Implemented vertical slice.
* Tech stack.
* How to run with Docker.
* How to run backend/frontend manually.
* How to run tests.
* Provider mock trigger values:

    * `valid`
    * `partial`
    * `invalid`
    * `unavailable`
    * `timeout`
* Key design decisions.
* Tradeoffs.
* Deferred work.
* What would be done with another day.
* Link to AI log.

## AI_LOG

Make sure it is an index into:

```text
docs/agents/tasks/
docs/agents/reports/
```

It should briefly explain the workflow:

* planning
* implementation
* review
* corrections
* documentation

## Future Forwards

Must include:

* encrypted credential storage / credential vault
* real Provider HTTP client
* split transaction around real Provider call
* frontend tests
* Testcontainers
* auth
* dynamic workflow definition
* CI

## Validation

Check all links.

Run:

```bash
docker compose up --build
cd backend && ./gradlew test
cd frontend && npm run build
```

## Report

Create:

```text
docs/agents/reports/011-documentation-finalization.md
```

Capability:

```text
Evaluator-ready documentation finalized.
```
