# Future Forwards

Items deferred from the current implementation slice. These are not planned for the submitted MVP but are documented here to preserve intent.

## Infrastructure

- Add backend service to Docker Compose.
- Add frontend service to Docker Compose.
- Add CI pipeline (lint, test, build).
- Add production Docker images (multi-stage builds).
- Add separate mock Provider HTTP service.

## Backend

- Encrypt Provider credentials at rest using KMS or secrets-manager-backed strategy.
- Add Testcontainers for PostgreSQL integration tests.
- Load workflow definitions from the database (`DatabaseWorkflowDefinition`).
- Add versioned JSON Schema validation for step payloads.
- Generate OpenAPI spec and TypeScript types from it.

## Frontend

- Generate TypeScript types from OpenAPI spec.
- Improve UI accessibility and validation messaging.
- Add Playwright end-to-end wizard smoke tests.

## Auth

- Add authentication and authorization.
- Add multi-partner tenancy.
