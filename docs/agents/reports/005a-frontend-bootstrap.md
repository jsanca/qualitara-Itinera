# Report: Frontend Bootstrap

## Summary

Initialized a React + TypeScript frontend scaffold using Vite inside `frontend/`. The scaffold provides the folder structure, config files, type placeholders, API placeholder module, and a minimal shell page that renders "Backend integration pending." No real wizard behavior or backend calls are implemented.

## Deliverables

### Project Config

| File | Purpose |
|---|---|
| `package.json` | React 18 + Vite 6, no UI libraries |
| `vite.config.ts` | Vite with React plugin |
| `tsconfig.json` | Strict TypeScript, bundler module resolution |
| `index.html` | Entry point with `root` div |

### Source Structure

| File | Purpose |
|---|---|
| `src/main.tsx` | React 18 root mount |
| `src/App.tsx` | Renders `OnboardingPage` |
| `src/pages/OnboardingPage.tsx` | Placeholder page: title + "Backend integration pending" |
| `src/components/StepShell.tsx` | Step navigation shell with `Details → Validation → Review` flow indicator |
| `src/types/onboarding.ts` | TypeScript types aligned with backend enums and DTOs |
| `src/api/onboardingApi.ts` | Placeholder functions that throw `"Not implemented yet"` |
| `src/styles/app.css` | Minimal CSS: step nav, typography, layout |

### Types Created

`OnboardingStepKey`, `OnboardingSessionStatus`, `OnboardingStepStatus`, `ProviderValidationOutcome`, `AllowedAction`, `DetailsPayload`, `ValidationPayload`, `ReviewPayload`, `OnboardingSessionResponse`, `ProviderItem` — all aligned with backend names, expect final adjustments when API contract lands.

### API Placeholder Functions

`createSession`, `getSession`, `submitDetails`, `startValidation`, `goLive` — all throw `new Error('Not implemented yet')`. Parameters are prefixed `_` to satisfy `noUnusedParameters` in strict TS config.

## Validation

```bash
cd frontend
npm install
npm run build
```
Build succeeds: `✓ built in 1.75s`, no TypeScript errors.

## Tests

No test suite configured yet (`npm test` not defined). This is consistent with the bootstrap scope.

## Engineering Notes

- No routing, state management, or UI libraries — scope excluded by task constraints.
- `StepShell` accepts `currentStep` as a prop so it can be driven by backend state once the API is available.
- The step nav uses plain CSS (`::after` for arrows) rather than an icon library.

## Tradeoffs

- **CSS modules vs. plain CSS**: Plain `app.css` is used per the "simple CSS or default styling only" constraint. CSS modules can be adopted when component complexity grows.
- **No environment config**: No `.env` or `VITE_API_URL` is wired yet. The `onboardingApi.ts` placeholder will need base URL injection when real endpoints are added.

## Follow-ups

- Wire `onboardingApi.ts` to real endpoints once the backend REST API contract is finalized (Phase 6 in the plan).
- Replace placeholder content in `OnboardingPage` with real step components.
- Add `localStorage` session-id persistence when session-resume behavior is implemented.
- Consider adding Playwright smoke tests when the full wizard is wired.
