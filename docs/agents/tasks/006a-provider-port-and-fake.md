# Mini Task 006A — Provider Port and Fake Client

## Goal

Implement the Provider validation boundary without touching onboarding persistence orchestration.

## Scope

Create:

* `ProviderValidationPort`
* `FakeProviderValidationClient`
* `ProviderValidationRequest`
* `ProviderValidationResult`
* result variants for:

    * VALID
    * PARTIAL
    * INVALID
    * UNAVAILABLE
    * TIMEOUT

## Fake Trigger Values

Use deterministic inputs:

* `accountId=valid` → valid + items
* `accountId=partial` → partial + items + warnings
* `accountId=invalid` → invalid + reason
* `accountId=unavailable` → unavailable
* `accountId=timeout` → timeout

## Tests

Add pure unit tests for all five outcomes.

## Constraints

Do not write to DB.
Do not implement REST.
Do not change workflow service.
Do not log API keys.

## Report

Create:

* `docs/agents/tasks/006a-provider-port-and-fake.md`
* `docs/agents/reports/006a-provider-port-and-fake.md`

Capability:

`Provider integration boundary established.`
