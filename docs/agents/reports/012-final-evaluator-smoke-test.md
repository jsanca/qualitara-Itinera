# Report 012 — Final Evaluator Smoke Test and README Review

## Summary

Project validated from the evaluator's perspective. Docker build/start, Flyway validation, backend startup, frontend delivery, five Provider outcomes, reload/resume, go-live, and secret redaction were exercised against the real Compose stack and browser UI.

Six of the seven requested smoke-test areas pass. The `UNAVAILABLE` state and retry action work, but the full `UNAVAILABLE → edit details → VALID` recovery cannot be completed through the current UI because it does not render the backend-provided `EDIT_DETAILS` action from Validation. This was documented rather than changed because Task 012 is explicitly not a code task.

## Environment

```text
Host: macOS / Docker via OrbStack
Command: docker compose up --build -d
PostgreSQL: 16.14, healthy
Backend runtime: Java 21.0.11 / Spring Boot 3.5.0
Frontend: nginx serving the Vite production build on http://localhost:3000/
Database schema: Flyway migration V1 validated; schema current at version 1
Browser: in-app Chromium against the real localhost UI
Smoke API key: test-key (new-key for credential-change recovery)
```

The evaluator stack remains running after validation. No database volume was deleted.

## Smoke Test Results

| # | Scenario | Result | Evidence |
|---|---|---|---|
| 1 | Happy path — `VALID` | PASS | Review displayed Primary and Secondary Feed; go-live displayed “Onboarding Complete” and live welcome text. PostgreSQL recorded `COMPLETE / LIVE / VALID`. |
| 2 | `PARTIAL` | PASS | Review displayed Primary Feed plus “Secondary Feed is paused” and “Rate limit at 80% capacity”; go-live succeeded. PostgreSQL recorded `COMPLETE / LIVE / PARTIAL`. |
| 3 | `INVALID` | PASS | Wizard returned to Details, displayed the credential-rejection reason, preserved safe details, cleared the raw key field, and exposed no Go Live button. |
| 4 | `UNAVAILABLE → Retry` | PARTIAL | `UNAVAILABLE` message and `Retry Validation` appeared; persisted state is `VALIDATION / DRAFT / UNAVAILABLE`. The UI has no Edit Details control from Validation, so changing `accountId` to `valid` is not possible through the wizard. A credential change performed from Details in the INVALID recovery did display `STALE`, then validate and go live successfully. |
| 5 | `TIMEOUT` | PASS | Timeout message and `Retry Validation` appeared; reload restored the same retryable state. PostgreSQL recorded `VALIDATION / DRAFT / TIMEOUT`. |
| 6 | Resume | PASS | Browser reloads at Details, Validation, Review, Complete, and Timeout returned to the corresponding backend state. |
| 7 | Sensitive data | PASS | Raw keys and credential fingerprints were absent from rendered page text and PostgreSQL payload/audit data. Only masked values such as `****-key` were shown. The transient retry password control retains the entered value visually masked until reload/unmount; it is not persisted. |

No browser console warnings or errors were recorded during the tested paths.

## Documentation Findings

1. The README had run/build instructions and Provider triggers, but no compact evaluator-oriented manual checklist.
2. Documentation described credential changes from Review/Validation as a supported workflow transition without distinguishing the backend capability from the controls actually exposed by the minimal UI.
3. The README said the raw API key was held only for the active request. That is true for the backend, but the frontend retains the password-field value after a transient failure for immediate retry.
4. The implementation plan described flow verification as fully complete despite the missing UI route for `EDIT_DETAILS` in transient states.
5. All other reviewed documents matched current implementation terminology and boundaries after Task 011: `COMPLETE` is the terminal step, `LIVE` is lifecycle status, `/validation` accepts an API key body, and raw credentials are not persisted.

## Corrections Made

- Added a `Manual Evaluation` section to `README.md` covering all Provider triggers, reload/resume, and sensitive-data expectations.
- Clarified frontend versus backend API-key lifetime and masked password-control behavior.
- Documented the missing Validation/Review Edit Details control as a known UI limitation in README and architecture.
- Added the missing `EDIT_DETAILS` UI action and browser coverage to `docs/FUTURE_FORWARDS.md`.
- Updated `docs/PLAN.md` so flow verification records the backend coverage and remaining UI gap rather than claiming an unconditional pass.
- Added Task 012 to `AI_LOG.md`.

No Kotlin, TypeScript, SQL, configuration, or runtime code was changed.

## Validation

```text
docker compose up --build -d
→ backend and frontend images built; all three services started

docker compose ps
→ PostgreSQL healthy; backend and frontend up

backend logs
→ Flyway validated migration V1; schema version 1; Tomcat started on :8080

Browser smoke test
→ VALID, PARTIAL, INVALID, UNAVAILABLE, TIMEOUT, resume, and redaction paths exercised

PostgreSQL secret check
→ step_payloads=0 matches for raw "test-key"
→ attempt_payloads=0 matches for raw "test-key"
→ no raw api_key column exists
```

Local Markdown links and JSON contract examples were rechecked after documentation corrections; `git diff --check` passed.

## Engineering Notes

**UI gap is presentation, not domain policy.** `AllowedActionPolicy` returns `EDIT_DETAILS`, and `PUT /details` supports credential changes and `STALE`. `OnboardingPage` renders components solely from `currentStep`, while `ValidationStep` and `ReviewStep` have no edit callback/control. The missing control is therefore isolated to frontend orchestration.

**Sensitive value nuance.** After `UNAVAILABLE` or `TIMEOUT`, the password input retains the raw value in React component state to support immediate retry. Its input type is `password`, it is absent from rendered page text, and reload clears it. PostgreSQL and API projections retain only masked/fingerprint data. This satisfies visible redaction, but the shorter in-memory lifetime described previously was inaccurate and has been corrected.

**Evaluator evidence.** UI success messages were cross-checked against persisted session state so `COMPLETE / LIVE` and the final validation statuses were verified explicitly rather than inferred only from screen copy.

## Tradeoffs

- The smoke pass did not mutate frontend code because the assigned task is documentation/validation only. This leaves one requested scenario partially blocked but preserves scope integrity.
- Existing sessions were created in the local evaluation database rather than deleting the developer volume. Each scenario used a new onboarding session, so prior rows did not affect behavior.
- Browser checks focused on functional state and visible text rather than visual-polish review, consistent with the project's stated non-goals.

## Remaining Observations

- Add an Edit Details action to Validation and Review, wired to a UI state that can render `DetailsStep` without inventing a backend transition. This is the only blocker found in the requested evaluator flow.
- Consider displaying a small explicit `VALID`/`PARTIAL` badge on Review. Today items/warnings and navigation make the result clear, but the exact validation status is not visibly labeled.
- Add browser automation to prevent regressions in transient recovery and reload/resume.

## Follow-ups

1. Implement and test frontend `EDIT_DETAILS` navigation in a separate code task.
2. Re-run only the `UNAVAILABLE → edit → STALE → VALID → go-live` browser scenario after that fix.
3. Add Playwright coverage for all seven evaluator paths and integrate it into CI when CI is introduced.
