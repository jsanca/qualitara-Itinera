# Future Forwards

The submitted vertical slice is complete. These items are intentionally deferred so the current implementation stays focused and inspectable.

## Security and Product Boundary

- Add authentication, authorization, and partner-scoped access control.
- Add multi-partner tenancy and audit access policy.
- Integrate encrypted credential storage or a credential vault backed by KMS/secrets management. The current slice deliberately does not persist raw API keys.
- Define credential rotation as a separate post-onboarding workflow; completed sessions remain terminal.

## Provider Integration

- Replace `FakeProviderValidationClient` with a real Provider HTTP client behind `ProviderValidationPort`.
- Split the current validation transaction around the real network call: commit `PENDING`, call the Provider without holding a database transaction, then persist the attempt and final outcome in a new transaction.
- Add explicit HTTP timeouts, retry/backoff policy, correlation IDs, metrics, and structured transport-error mapping.
- Add contract tests using a stub server such as WireMock. Keep the in-process fake for deterministic domain and UI development.

## Workflow and API

- Load dynamic workflow definitions from persistence only if partner-specific flows become a real requirement.
- Generate OpenAPI and TypeScript types instead of maintaining backend/frontend DTOs manually.
- Add versioned JSON Schema validation and migration tooling for stored JSONB payloads.
- Introduce more specific domain error types or RFC 9457 problem details if clients need richer recovery behavior.

## Testing

- Add Testcontainers so PostgreSQL-backed backend tests need no pre-running local database.
- Add frontend unit/component tests with Vitest and Testing Library.
- Add Playwright end-to-end tests for resume, valid/partial completion, invalid correction, and transient retry flows.
- Add concurrency tests around validation attempt numbering and simultaneous go-live requests.

## Delivery and Operations

- Add CI for backend tests, frontend build/tests, link checks, and container builds.
- Harden images for production: pin base-image digests, run as non-root, add image scanning, runtime health checks, and production configuration/secrets handling.
- Add observability appropriate for a real deployment: metrics, tracing, structured logging, and alerting.
- Separate local/development Compose configuration from deployment infrastructure if the service moves beyond evaluation use.
