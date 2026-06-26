# CLAUDE.md

## Project: Gradus

Gradus is a resumable partner onboarding platform implemented as a production-quality vertical slice.

Although this repository is being built under a constrained evaluation window, treat it as a real system. Prefer correctness, explicit tradeoffs, testability, and maintainability over feature breadth.

## Product Goal

A partner company can self-onboard by:

1. Entering company and Provider credentials.
2. Validating the Provider integration.
3. Reviewing discovered Provider items and going live.

The flow must be resumable. The backend is the source of truth for session state, current step, validation result, and allowed actions.

## Core Engineering Principles

* Backend owns workflow state.
* Frontend renders backend state; it does not invent transitions.
* Keep the business flow fixed for this slice, but keep the code extensible.
* Use PostgreSQL + Flyway migrations.
* Persist step state using a hybrid relational + JSONB model.
* Validate JSONB payloads through typed Kotlin DTOs and application-level validation.
* Provider integration must sit behind a port.
* Use an in-process fake Provider for the MVP.
* Go-live must be idempotent and transactional.
* Tests should focus on the meaningful parts: state transitions, validation outcomes, idempotency, and persistence.

## Explicit Non-Goals

Do not implement:

* Authentication/login.
* Real third-party Provider integration.
* Dynamic form engine.
* Kubernetes, CI, production infra, or multi-stage Docker builds.
* Visual polish beyond a clear usable wizard.
* AI crawling or advanced automation.

## Preferred Stack

Backend:

* Kotlin
* Spring Boot
* PostgreSQL
* Flyway
* NamedParameterJdbcTemplate
* Jackson
* JUnit

Frontend:

* React
* TypeScript
* Simple component-local or lightweight state management
* Backend DTO-aligned types

## Documentation Expectations

Keep these updated:

* `README.md`
* `ARCHITECTURE.md`
* `AI_LOG.md`
* `docs/adr/`
* `docs/FUTURE_FORWARDS.md`
* `docs/agents/tasks/`
* `docs/agents/reports/`

Every agent task should end with a report describing:

* What changed.
* Why it changed.
* Tests run.
* Problems found.
* Any assumptions made.
* Anything that needs human review.

## AI Work Rules

AI may generate code, but humans own the design.

Agents must not silently expand scope. If something is outside the current task, document it as a recommendation or future-forward instead of implementing it.

When uncertain, prefer the simplest implementation that preserves the architecture.

