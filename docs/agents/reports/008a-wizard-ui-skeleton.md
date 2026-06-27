# Report: Wizard UI Skeleton Against API Contract

## Summary

Built the three-step wizard UI skeleton (`DetailsStep`, `ValidationStep`, `ReviewStep`) driven by session state, with mock data. No real backend calls. Types were updated to match `docs/API_CONTRACT.md`. The page includes a demo step switcher (clearly marked) so the three steps can be previewed without backend integration.

## Deliverables

### Types Updated (`src/types/onboarding.ts`)

Revised all types to align with the API contract:

| Type | Change |
|---|---|
| `AllowedAction` | Values changed from snake-case (`submit_details`) to SCREAMING_SNAKE (`SUBMIT_DETAILS`) per contract |
| `ValidationStatus` | Added all 8 statuses including `PENDING` and `STALE` |
| `DetailsSummary` | Renamed from `DetailsPayload`; added `accountId`, `apiKeyPresent`, `apiKeyMasked` per contract |
| `ValidationSummary` | Replaced `ValidationPayload`; contains `status`, `items`, `warnings`, `reason` per contract |
| `ProviderItem` | Fields changed to `externalId`, `name`, `status` per contract |
| `OnboardingSessionResponse` | Uses `sessionStatus` (not `status`) and `validationStatus` (not `validation.status`) per contract |
| `SubmitDetailsRequest` | New type for the details form submission |

### Components Added

| File | Description |
|---|---|
| `src/components/steps/DetailsStep.tsx` | Form with `companyName`, `accountId`, `apiKey` fields; shows `apiKeyMasked` when key is stored; submit button disabled by `allowedActions` |
| `src/components/steps/ValidationStep.tsx` | Status panel showing human-readable message per `validationStatus`; validate/retry button gated by `allowedActions` |
| `src/components/steps/ReviewStep.tsx` | Items list with status badges; warnings list (shown only when `PARTIAL`); Go Live button gated by `allowedActions` |

### OnboardingPage Updated

`src/pages/OnboardingPage.tsx` now:
- Renders the correct step component based on `session.currentStep`
- Contains `MOCK_SESSIONS` with all four step states (DETAILS, VALIDATION, REVIEW, COMPLETE)
- Includes a demo step switcher (clearly labeled "Demo step:") to preview each state without backend

### CSS Extended

`src/styles/app.css` extended with styles for: form fields, disabled buttons, status panels (color-coded by status), items list with status badges, warnings list, demo controls bar.

## Validation

```bash
cd frontend && npm run build
```
Build succeeds: `✓ built in 2.14s`, no TypeScript errors.

## Engineering Notes

- The demo step switcher is implemented as local component state with no side effects — it does not call any API functions and does not persist. It is clearly labeled as demo-only.
- Buttons are gated by `allowedActions` so they reflect what the backend will permit, even in the mock state.
- `ValidationStep` maps all 8 `ValidationStatus` values to human-readable messages.
- The REVIEW step renders a `COMPLETE` panel when `currentStep === 'COMPLETE'` rather than using a fourth step component (the flow ends at COMPLETE).

## Tradeoffs

- **Demo step switcher vs. no interactivity**: The task allowed "buttons may be disabled or wired to local mock transitions only if clearly marked as temporary." The step switcher is the most practical way to demonstrate all three step UIs without a wired backend.
- **One ReviewStep vs. separate SuccessStep**: The COMPLETE state is rendered inline in `OnboardingPage` rather than a separate component since no further user action is needed.

## Follow-ups

- Replace `MOCK_SESSIONS` with real API calls once `onboardingApi.ts` is implemented.
- Add `localStorage` session-id persistence for reload-resume (Phase 7 in plan).
- Wire form submit and button click handlers to call the actual API functions.
