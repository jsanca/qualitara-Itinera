# Report: Provider Port and Fake Client

## Summary

Implemented the Provider validation boundary inside `backend/src/main/kotlin/com/qualitara/itinera/provider/`. Created the port interface, request/result DTOs, a fake in-process client with all five deterministic outcomes, and a transport exception type. No persistence, REST, or workflow orchestration was modified.

## Deliverables

### New Package: `com.qualitara.itinera.provider`

| File | Description |
|---|---|
| `ProviderValidationRequest.kt` | Request DTO: `accountId`, `apiKey` |
| `ProviderValidationResult.kt` | Result DTO: `outcome`, `items`, `warnings`, `reason` |
| `ProviderValidationPort.kt` | Interface with single `validate(request): ProviderValidationResult` method |
| `FakeProviderValidationClient.kt` | In-process fake implementation with 5 deterministic outcomes |
| `FakeProviderValidationClientTest.kt` | 6 unit tests covering all 5 outcomes plus unknown accountId |

### Port Interface

```kotlin
interface ProviderValidationPort {
    fun validate(request: ProviderValidationRequest): ProviderValidationResult
    throws ProviderValidationException  // transport failures only
}
```

### Deterministic Trigger Values

| `accountId` value | Outcome | Notes |
|---|---|---|
| `valid` | `VALID` | 2 items |
| `partial` | `PARTIAL` | 1 item, 2 warnings |
| `invalid` | `INVALID` | reason: "Credentials do not match..." |
| `unavailable` | `UNAVAILABLE` | no items, no reason |
| `timeout` | throws `ProviderValidationException(TIMEOUT)` | transport error |
| *(unknown)* | `INVALID` | reason: "Unknown account ID: ..." |

### Exception Type

`ProviderValidationException(RuntimeException)` carries a `outcome` field so callers can map transport failures to the correct workflow status without re-interpreting the error message.

## Validation

```
cd backend && ./gradlew test
```
41 tests — all pass (13 backend repository tests + 22 workflow tests + 6 new provider tests).

## Tests

| Test | Covered outcome |
|---|---|
| `valid accountId returns VALID outcome with items` | `VALID` |
| `partial accountId returns PARTIAL outcome with items and warnings` | `PARTIAL` |
| `invalid accountId returns INVALID outcome with reason` | `INVALID` |
| `unavailable accountId returns UNAVAILABLE outcome with no items or reason` | `UNAVAILABLE` |
| `timeout accountId throws ProviderValidationException with TIMEOUT outcome` | `TIMEOUT` (via exception) |
| `unknown accountId returns INVALID with unknown account reason` | fallback case |

## Engineering Notes

- The port is injected via Spring `@Component` so a real `RestClient` implementation can replace `FakeProviderValidationClient` without changing callers.
- `ProviderValidationException` extends `RuntimeException` and carries the `outcome` so the caller (typically a service layer) can map it to `ValidationStatus.UNAVAILABLE` or `ValidationStatus.TIMEOUT` without parsing message strings.
- The fake client produces `ProviderItem` objects with `externalId`, `name`, and `status` fields matching the existing `ProviderItem` data class in `workflow.payload`.
- `apiKey` is accepted but never logged or used in the fake implementation.

## Tradeoffs

- **Exception vs. result for TIMEOUT**: The task specifies TIMEOUT as an outcome value, but the transport-layer failure (timeout) is semantically distinct from an application-level result. The implementation uses a transport exception (`ProviderValidationException`) thrown from the port, carrying `TIMEOUT` as its `outcome` field. This separates transport errors from Provider-level responses while still preserving the semantic outcome for workflow mapping.
- **Unknown accountId → INVALID**: Rather than throwing an exception for an unknown accountId, the fake returns `INVALID` with a descriptive reason. This mirrors how a real Provider would likely respond to an unrecognized account and keeps the port interface free of exceptional cases for business-level "not found" responses.

## Follow-ups

- Wire `ProviderValidationPort` into the workflow service's validation call path (Phase 4/5).
- Add a `RestClient`-based implementation when the real Provider HTTP endpoint is available.
- Consider adding a timeout configuration parameter to `ProviderValidationRequest` if the real Provider has configurable timeouts.
