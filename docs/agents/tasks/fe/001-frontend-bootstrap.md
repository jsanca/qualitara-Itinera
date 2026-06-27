# Mini Task — Frontend Bootstrap

## Context

Project: Itinera

The backend workflow domain is still being finalized. This task prepares the React frontend structure without depending on completed REST endpoints.

Do not implement the full wizard logic yet.

## Goal

Create a minimal React + TypeScript frontend scaffold that can later consume the Itinera backend API.

## Scope

Inside `frontend/`, create or initialize:

* React + TypeScript app using Vite
* basic folder structure
* minimal app shell
* API client placeholder
* type placeholders aligned with the planned API contract
* simple CSS or default styling only

Suggested structure:

```text
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/
    │   └── onboardingApi.ts
    ├── components/
    │   └── StepShell.tsx
    ├── pages/
    │   └── OnboardingPage.tsx
    ├── types/
    │   └── onboarding.ts
    └── styles/
        └── app.css
```

## Requirements

The app should render a simple placeholder page:

```text
Itinera Partner Onboarding
Backend integration pending.
```

Add a simple step shell placeholder showing:

```text
Details → Validation → Review
```

## API Placeholder

Create `src/api/onboardingApi.ts` with placeholder functions only:

* `createSession`
* `getSession`
* `submitDetails`
* `startValidation`
* `goLive`

These may throw `new Error("Not implemented yet")` or return mocked placeholder data, but do not invent final endpoint behavior beyond the planned names.

## Types Placeholder

Create minimal TypeScript types matching current backend concepts:

* `OnboardingStepKey`
* `ValidationStatus`
* `AllowedAction`
* `OnboardingSessionResponse`
* `DetailsPayload`
* `ValidationPayload`
* `ProviderItem`

Keep these aligned with the backend names, but expect final adjustments after the API contract lands.

## Constraints

Do not:

* implement real wizard behavior
* call real backend endpoints
* add complex state management
* add routing unless trivial
* add UI libraries
* add auth
* add Provider logic
* add visual polish beyond clarity

## Validation

Run:

```bash
cd frontend
npm install
npm run build
```

If a test script exists, run it.

## Documentation

Create:

```text
docs/agents/tasks/005a-frontend-bootstrap.md
docs/agents/reports/005a-frontend-bootstrap.md
```

Update:

```text
AI_LOG.md
frontend/README.md
```

## Report

Use the standard report structure.

Capability added:

```text
Frontend workspace established for the onboarding wizard.
```
