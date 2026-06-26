# Task 001 — Repository Bootstrap

## Context

Project: Itinera

Itinera is a resumable partner onboarding platform implemented as a Kotlin + React + PostgreSQL vertical slice.

Treat this as a real engineering project, not as a disposable take-home. Keep the structure clean, minimal, and extensible.

## Goal

Create the initial monorepo structure with backend, frontend, documentation folders, and a simple Docker Compose setup for PostgreSQL.

Do not implement business logic in this task.

## Scope

Create:

```text
.
├── AGENTS.md
├── README.md
├── ARCHITECTURE.md
├── AI_LOG.md
├── docker-compose.yml
├── backend/
├── frontend/
└── docs/
    ├── PLAN.md
    ├── FUTURE_FORWARDS.md
    ├── adr/
    └── agents/
        ├── tasks/
        └── reports/
```

## Backend

Create the `backend/` folder only.

Do not scaffold the Spring Boot application yet unless a build tool requires minimal placeholder files.

Expected for this task:

```text
backend/
└── README.md
```

The backend README should briefly say this module will contain the Kotlin Spring Boot API.

## Frontend

Create the `frontend/` folder only.

Do not scaffold the React application yet unless a build tool requires minimal placeholder files.

Expected for this task:

```text
frontend/
└── README.md
```

The frontend README should briefly say this module will contain the React wizard UI.

## Docker Compose

Create a simple `docker-compose.yml` with PostgreSQL only.

Use:

* service name: `postgres`
* image: `postgres:16`
* database: `itinera`
* username: `itinera`
* password: `itinera`
* port: `5432:5432`
* named volume: `itinera-postgres-data`

Example environment:

```yaml
POSTGRES_DB: itinera
POSTGRES_USER: itinera
POSTGRES_PASSWORD: itinera
```

Do not add backend or frontend services yet.

## Documentation

Create or update these files:

### `AI_LOG.md`

This file should act as an index, not a raw prompt dump.

It should explain that detailed AI work is tracked through:

```text
docs/agents/tasks/
docs/agents/reports/
```

Add an entry for this task pointing to:

```text
docs/agents/tasks/001-repository-bootstrap.md
docs/agents/reports/001-repository-bootstrap.md
```

### `docs/agents/tasks/001-repository-bootstrap.md`

Copy this task into that file.

### `docs/agents/reports/001-repository-bootstrap.md`

Create the report after implementation.

Report must include:

* Summary
* Files created/changed
* Validation performed
* Tests run
* Notes / assumptions
* Follow-ups

### `docs/FUTURE_FORWARDS.md`

Create a placeholder with future items:

* Add backend service to Docker Compose.
* Add frontend service to Docker Compose.
* Add CI.
* Add production Docker images.
* Add separate mock Provider service.

## Constraints

* Keep this task small.
* Do not implement backend business logic.
* Do not implement frontend UI.
* Do not add Kubernetes, CI, or production Docker setup.
* Do not create a separate Provider service.
* Do not add unnecessary dependencies.

## Validation

After the task is complete, verify:

```bash
docker compose config
```

If Docker is available, also verify:

```bash
docker compose up -d postgres
docker compose ps
docker compose down
```

## Expected Report

The report should clearly state whether Docker Compose validation passed.

If Docker is not available locally, say so explicitly and report that only static validation was performed.

## Success Criteria

* Monorepo structure exists.
* Backend and frontend folders exist.
* Documentation folders exist.
* Docker Compose defines PostgreSQL correctly.
* `AI_LOG.md` indexes the task and report.
* No business logic was implemented.
