# Report 009 — Frontend Backend Integration

## Summary

Frontend wizard now drives the onboarding flow through backend state. Mock session data and demo step controls are replaced by real API calls. The wizard bootstraps from `localStorage`, resumes on reload, and every action (details submission, validation, go-live) is driven by the `allowedActions` array returned by the backend. Raw API keys are never stored beyond the in-flight request.

## Deliverables

- **`api/onboardingApi.ts`** — implemented all five API client functions with uniform error extraction: parses the backend `{ code, message }` DTO and throws it as a plain object so callers can surface the `message` to the user.
- **`types/onboarding.ts`** — added `ApiError` interface.
- **`pages/OnboardingPage.tsx`** — replaced mock data and demo controls with real backend-driven state management: `bootstrap()` on mount, `handleDetailsSubmit`, `handleValidate`, `handleGoLive` handlers, error banner, and "Start New Session" shortcut on COMPLETE.
- **`components/steps/DetailsStep.tsx`** — added `onSubmit`, `isLoading`, and `validationReason` props; wired the form submit; clears the API key field after submission; shows the validation rejection reason (from `validation.reason`) when the session returns to DETAILS after an INVALID outcome.
- **`components/steps/ValidationStep.tsx`** — added API key input field (re-entry required because raw keys are not stored), `onValidate`/`isLoading` props; button label changes between "Validate Credentials" and "Retry Validation" based on `allowedActions`.
- **`components/steps/ReviewStep.tsx`** — added partner details summary table, `onGoLive`/`isLoading` props; button fires go-live call.
- **`styles/app.css`** — added error banner styles and summary table styles; removed demo-only styles (`.demo-controls`, `.demo-btn`) that are no longer used.
- **`vite.config.ts`** — added dev proxy (`/api` → `http://localhost:8080`) so the frontend dev server forwards API calls to the backend without CORS configuration.

## Validation

Build:
```bash
cd frontend && npm run build
# ✓ built in 772ms — 0 TypeScript errors
```

Manual flow verification (requires `docker compose up -d && cd backend && ./gradlew bootRun`):

1. Fresh load (no localStorage) → session created, DETAILS step shown
2. Reload → session resumed from `itinera.sessionId`
3. Submit details (`accountId=valid`) → advances to VALIDATION step
4. Enter API key + validate → VALID outcome, advances to REVIEW
5. Review shows company details, items (feed-001, feed-002)
6. Go Live → COMPLETE, welcome message shown
7. Repeat with `accountId=partial` → PARTIAL + warnings visible in REVIEW
8. Repeat with `accountId=invalid` → INVALID, returns to DETAILS with rejection reason
9. Repeat with `accountId=unavailable` → UNAVAILABLE, RETRY_VALIDATION action available
10. Repeat with `accountId=timeout` → TIMEOUT, RETRY_VALIDATION action available
11. Submit changed credentials after VALID → STALE shown in VALIDATION step

## Tests

No automated frontend tests — the task spec does not require them and the project has no frontend test framework configured. The manual browser validation above covers the golden path and all non-success outcomes.

Backend API tests (85 tests, 0 failures) cover the contract the frontend depends on.

## Engineering Notes

**Session bootstrap pattern.** On mount, the page reads `itinera.sessionId` from localStorage. If present, it calls `GET /sessions/{id}`; on 404, it removes the stale key and creates a new session. Any other error surfaces in the error banner. This covers cold start, reload-resume, and stale session recovery without routing or complex state management.

**Why the API key is re-entered for validation.** The backend does not persist the raw API key (only a SHA-256 fingerprint). Each validation call requires the key to be supplied in the request body. The ValidationStep shows a password field on every attempt. After a successful validation (VALID/PARTIAL), the ValidationStep unmounts as the session moves to REVIEW and the field state is discarded. After a retry (UNAVAILABLE/TIMEOUT), the field persists for immediate re-use.

**Error extraction.** The `request()` helper in `onboardingApi.ts` checks `res.ok`; on failure it parses the JSON body as `{ code, message }` and throws it. The page catches errors in each handler and extracts `message` for the banner. The `code` is not shown to the user (it's for programmatic use), only `message`.

**CORS solved by Vite proxy.** Rather than configuring Spring CORS (which would require a separate annotation or filter), the Vite dev server proxies `/api` to `http://localhost:8080`. This works for development; production would co-locate the frontend and backend or use a reverse proxy.

**INVALID session step returns to DETAILS.** When the Provider rejects credentials, the backend sets `currentStep = DETAILS`. The page renders `DetailsStep` with `validationReason = session.validation.reason`, showing the rejection message above the form so the user understands why they're back.

## Tradeoffs

- No frontend test framework. Manual browser validation is the only coverage for the UI. A Vitest + Testing Library setup would catch regression, but was out of scope per task constraints ("no complex state management", "no auth", no framework additions).
- Demo controls removed. The `MOCK_SESSIONS` block and step-toggle buttons are gone. Any future manual UI testing happens through real backend calls.
- "Start New Session" button on COMPLETE clears `localStorage` and re-bootstraps. This is a convenience for development/testing; a real product would not expose this.

## Follow-ups

The full vertical slice (persistence → workflow → validation → go-live → REST API → React wizard) is now complete for the development scenario. Future tasks could include: real Provider HTTP integration, authenticated sessions, production build integration, and frontend testing infrastructure.
