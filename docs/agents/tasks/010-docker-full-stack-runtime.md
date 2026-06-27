# Task 010 — Docker Full-Stack Runtime

## Goal

Make Itinera runnable with Docker Compose as a complete local stack.

## Scope

Add:

* `backend/Dockerfile`
* `frontend/Dockerfile`
* update root `docker-compose.yml`

Services:

```text
postgres
backend
frontend
```

## Requirements

### Backend

* Build Spring Boot app using Gradle wrapper.
* Run backend on port `8080`.
* Connect to Postgres using Docker service hostname:

```text
jdbc:postgresql://postgres:5432/itinera
```

* Use env vars for datasource config.

### Frontend

* Build React app.
* Serve it with a simple static server, preferably nginx.
* Expose frontend on port `5173` or `3000`.
* Configure frontend API base path to call backend through Docker networking or nginx proxy.

### Docker Compose

Root command should be:

```bash
docker compose up --build
```

Evaluator should be able to open the frontend and use the app.

## Constraints

Do not add Kubernetes, CI, production deployment, or complex infra.

Keep Docker simple and local-dev focused.

## Validation

Run:

```bash
docker compose up --build
```

Then verify:

* Postgres starts.
* Backend starts.
* Flyway runs.
* Frontend loads.
* Wizard can call backend.

## Documentation

Update:

* `README.md`
* `backend/README.md` if needed
* `frontend/README.md` if needed
* `AI_LOG.md`

Create report:

```text
docs/agents/reports/010-docker-full-stack-runtime.md
```

Capability:

```text
Full local stack runnable through Docker Compose.
```
