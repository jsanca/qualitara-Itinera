# Task 004 — Workflow Domain

## Context

Project: Itinera

Phase 2 established the persistence foundation. Phase 3 introduces the onboarding workflow domain.

This task must implement the backend-owned workflow model, transition validation, allowed actions calculation, and typed step payload DTOs.

Do not implement REST controllers yet unless strictly needed for tests. Do not implement Provider validation behavior yet.

## Goal

Create the workflow domain layer that will become the source of truth for the onboarding wizard.

The backend must decide:

* current step
* valid transitions
* allowed actions
* whether validation state is still trustworthy
* what happens when credentials change

## Engineering Capability Added

Backend-owned onboarding workflow state machine.

## Layer Boundary Rules

This task works in the domain/service boundary, not persistence-only infrastructure.

Allowed responsibilities:

* workflow state model
* transition validation
* allowed action calculation
* typed payload DTOs
* credential-change detection
* invalidating stale validation state
* domain-specific exceptions
* domain tests

Forbidden responsibilities:

* REST controllers
* React/frontend code
* Provider client implementation
* real validation calls
* go-live transaction
* database schema changes
* dynamic workflow engine
* cache
* production observability infrastructure

## Workflow Steps

Use existing enum: DETAILS → VALIDATION → REVIEW → COMPLETE

## Validation Status

NOT_STARTED, PENDING, VALID, PARTIAL, INVALID, UNAVAILABLE, TIMEOUT, STALE

STALE means validation previously existed but credentials changed, so the result can no longer be trusted.

## Allowed Actions

SUBMIT_DETAILS, EDIT_DETAILS, START_VALIDATION, RETRY_VALIDATION, GO_TO_REVIEW, GO_LIVE

## Credential Change Rule

BR-001 — Editing credentials invalidates prior validation.

Use SHA-256 fingerprint of accountId + apiKey. Never store or log the raw API key.

## Exceptions

InvalidWorkflowTransitionException, UnsupportedPayloadVersionException

## Tests

Minimum 12 unit tests covering all step transitions, BR-001, retryable outcomes, go-live guard, and invalid transition rejection.

## Documentation Updates

Create: docs/agents/tasks/004-workflow-domain.md, docs/agents/reports/004-workflow-domain.md
Update: AI_LOG.md, docs/ARCHITECTURE.md
ADR: docs/adr/0002-backend-owned-static-workflow.md (already exists)

## Constraints

Do not implement REST endpoints, frontend, Provider fake, go-live transaction, JPA, or a dynamic workflow engine.
Do not log raw credentials or expose raw API keys in DTOs.
