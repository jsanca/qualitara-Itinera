# Itinera Implementation Plan

## Goal

Build a correct, resumable, idempotent onboarding vertical slice with Kotlin, React, and PostgreSQL. The backend remains the source of truth and the system stays small enough to evaluate end to end.

## Delivery Status

| Phase | Capability | Status | Evidence |
|---|---|---|---|
| 0 | Repository bootstrap | Complete | Tasks/reports `001` |
| 1 | Backend foundation | Complete | `002` |
| 2 | Persistence model | Complete | `003`, `003a–c` |
| 3 | Workflow domain | Complete | `004`, `004a` |
| 4 | Provider validation | Complete | `006a–c` |
| 5 | Transactional go-live | Complete | `007` |
| 6 | REST API | Complete | `005b`, `008` |
| 7 | Frontend wizard | Complete | `005a`, `008a`, `009` |
| 8 | Flow verification | Complete | backend tests and `009a` |
| 9 | Docker runtime | Complete | `010` |
| 10 | Documentation finalization | Complete | `011` |

## Implemented Scope

- Fixed backend-owned workflow: `DETAILS → VALIDATION → REVIEW → COMPLETE`.
- PostgreSQL lifecycle records plus typed, versioned JSONB step payloads.
- Deterministic fake Provider behind a port, with audit history and retryable outcomes.
- Transactional, idempotent go-live.
- Five session-oriented REST endpoints and uniform error responses.
- React wizard driven by API state, with session resume through `localStorage`.
- Docker Compose runtime for database, backend, and frontend.
- Domain, repository, Provider, go-live, API, and end-to-end backend coverage.

## Explicit Non-Goals

- authentication or login
- real third-party Provider integration
- dynamic form/workflow engine
- production deployment infrastructure or CI
- complex frontend state management or visual polish

Deferred extensions and the rationale for leaving them out are tracked in [Future Forwards](FUTURE_FORWARDS.md). Detailed delivery history is indexed in the [AI Log](../AI_LOG.md).
