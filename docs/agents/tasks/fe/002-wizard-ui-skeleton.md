# Mini Task — Wizard UI Skeleton Against API Contract

## Context

Project: Itinera

The frontend is already bootstrapped. REST endpoints are not implemented yet, but `docs/API_CONTRACT.md` defines the planned session response shape.

This task builds the wizard UI skeleton against mocked session data, without real backend calls.

## Goal

Create the three-step wizard UI structure so the frontend can be wired quickly once REST endpoints exist.

## Scope

Implement:

* `DetailsStep`
* `ValidationStep`
* `ReviewStep`
* session-state-driven rendering
* simple mock session data
* local component state only

## Requirements

Render based on:

* `currentStep`
* `validationStatus`
* `allowedActions`

Show:

* Details form fields
* Validation status panel
* Provider items placeholder
* Warnings area for PARTIAL
* Go-live button placeholder

## Constraints

Do not:

* call real backend
* implement localStorage resume yet
* invent business transitions locally
* add UI libraries
* add routing
* add auth
* add Provider logic

Buttons may be disabled or wired to local mock transitions only if clearly marked as temporary.

## Validation

Run:

```bash
cd frontend
npm run build
```

## Report

Create:

* `docs/agents/tasks/008a-wizard-ui-skeleton.md`
* `docs/agents/reports/008a-wizard-ui-skeleton.md`

Capability:

`Wizard UI skeleton prepared for backend integration.`
