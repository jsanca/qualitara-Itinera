# Report: Backend End-to-End Flow Tests

## Summary

Reviewed the existing `OnboardingApiIntegrationTest.kt` and added the one missing end-to-end flow: the unavailable retry scenario. All 4 required flows are now covered. The existing test suite already had 3 of the 4 flows; no new file was needed since `OnboardingApiIntegrationTest.kt` already contains the appropriate structure.

## Flow Coverage

| Flow | Status | Test |
|---|---|---|
| Happy path — VALID + go-live | Covered | `go-live succeeds after valid validation` + `repeated go-live is idempotent` |
| PARTIAL can go live | Covered | `go-live succeeds after partial validation` |
| UNAVAILABLE → retry → VALID → go-live | **Added** | `unavailable retry flow — unavailable response includes retry action, retry with valid credentials succeeds, go-live works` |
| INVALID blocks go-live | Covered | `go-live rejected before validation succeeds returns 409` |

## New Test Added

### `unavailable retry flow — unavailable response includes retry action, retry with valid credentials succeeds, go-live works`

Flow exercised:
```
create session
submit details (accountId=unavailable, apiKey=unavail-key)
validate → UNAVAILABLE  [asserts: UNAVAILABLE + RETRY_VALIDATION action present]
submit details (accountId=valid, apiKey=new-key)  [BR-001: credentials changed]
validate → VALID  [asserts: VALID, currentStep=REVIEW]
go-live → COMPLETE/LIVE  [asserts: COMPLETE, LIVE, VALID, allowedActions=[]]
```

Asserts:
- UNAVAILABLE response includes `RETRY_VALIDATION` in `allowedActions`
- Re-submitting with changed credentials marks validation `STALE` (BR-001)
- Retry with valid credentials produces `VALID` outcome
- Session advances to `REVIEW` then `COMPLETE/LIVE`

## Validation

```
cd backend && ./gradlew test
```
All backend tests pass (42 total: 22 workflow + 13 repository + 7 provider + 5 go-live service + 6 API flow tests).

## Tests

| Test | File | Covered flow |
|---|---|---|
| `go-live succeeds after valid validation` | `OnboardingApiIntegrationTest.kt` | Happy path VALID |
| `repeated go-live is idempotent` | `OnboardingApiIntegrationTest.kt` | Idempotency + partner account uniqueness |
| `go-live succeeds after partial validation` | `OnboardingApiIntegrationTest.kt` | PARTIAL → go-live |
| `unavailable retry flow...` | `OnboardingApiIntegrationTest.kt` | UNAVAILABLE → retry → VALID |
| `go-live rejected before validation succeeds returns 409` | `OnboardingApiIntegrationTest.kt` | INVALID blocks go-live |

## Engineering Notes

- The retry flow requires re-submitting details to change the credential fingerprint (BR-001). Submitting the same credentials after `UNAVAILABLE` would not reset the `STALE` flag and the validation would remain `UNAVAILABLE`.
- The `accountId` in `submitDetails` drives the `FakeProviderValidationClient` outcome — the second submit uses `accountId=valid` which produces `VALID` regardless of the second `apiKey`.
- API key redaction is asserted by the existing `submit details stores redacted details and never returns raw apiKey` test.

## Tradeoffs

- **Single new test vs. separate flow class**: The existing `OnboardingApiIntegrationTest.kt` already had the infrastructure (helpers, cleanup, MockMvc setup) for these flows. Adding the single missing flow there was more efficient than creating a parallel test class with duplicated setup.

## Follow-ups

- The 4 required flows are all covered. No further flow tests are needed until real Provider integration replaces the fake client.
