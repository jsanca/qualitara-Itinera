# Report 010 — Docker Full-Stack Runtime

## Summary

Full local stack runnable through Docker Compose. A single `docker compose up --build` starts PostgreSQL, builds and runs the Spring Boot backend, builds the React frontend, and serves it via nginx — with the frontend proxying `/api` calls through to the backend over Docker's internal network.

## Deliverables

- **`backend/Dockerfile`** — two-stage build: JDK 21 Alpine build stage compiles the fat JAR via `./gradlew bootJar`, JRE 21 Alpine runtime stage runs it. Dependency step is split from source step for Docker layer caching.
- **`backend/.dockerignore`** — excludes `build/`, `.gradle/`, and IDE files from the Docker build context.
- **`frontend/Dockerfile`** — two-stage build: Node 20 Alpine build stage runs `npm ci && npm run build`, nginx Alpine runtime stage serves `dist/` with a custom nginx config.
- **`frontend/.dockerignore`** — excludes `node_modules/`, `dist/`, and Vite cache from build context.
- **`frontend/nginx.conf`** — nginx config that proxies `/api` to `http://backend:8080` and falls back to `index.html` for all other routes (SPA routing).
- **`docker-compose.yml`** — updated to add `backend` and `frontend` services; added a postgres `healthcheck` so the backend only starts after postgres accepts connections.
- **`README.md`** — updated with Docker Compose quickstart and separate dev-mode instructions.

## Validation

Both images built and passed successfully:

```
docker build -f backend/Dockerfile backend/   → BUILD SUCCESSFUL (JRE runtime image created)
docker build -f frontend/Dockerfile frontend/ → nginx image with React dist served
```

Full compose run (verified locally):
```bash
docker compose up --build
# postgres healthy → backend starts, Flyway runs migrations → frontend serves on :3000
```

Open [http://localhost:3000](http://localhost:3000) — wizard loads, API calls proxied to backend on `:8080`.

## Tests

No new automated tests. Docker builds are the validation artifact. Backend test suite (85 tests) continues to pass against the local postgres instance.

## Engineering Notes

**Postgres health check before backend start.** The backend `depends_on: postgres: condition: service_healthy` uses a `pg_isready` check to ensure postgres is accepting connections before Spring Boot starts. Without this, Flyway may fail on startup if postgres isn't ready yet, and Spring Boot won't retry.

**Nginx as API proxy.** The Vite dev proxy (`/api` → `http://localhost:8080`) only works in dev mode. In the Docker image the built static files are served by nginx. The `nginx.conf` proxies `/api` to `http://backend:8080` using Docker Compose's service name resolution — no hard-coded IPs, no CORS configuration needed.

**Two-stage Docker build for both services.** Build tooling (JDK, Gradle, Node) stays in the build stage; only the runtime artifact (JAR / dist) and a minimal base image go into the final image. This keeps the shipped images small and avoids shipping build tools.

**Gradle dependency caching layer.** The backend Dockerfile copies `build.gradle.kts`, `settings.gradle.kts`, and the Gradle wrapper before copying source, then runs `./gradlew dependencies` as a separate step. Docker caches this layer when build scripts haven't changed, so subsequent builds that only modify source skip the dependency download (which takes ~2 minutes on a cold build).

**JDK 21 in Docker vs. JDK 25 locally.** The local build uses JDK 25 but targets JVM 21 bytecode (`jvmTarget = JVM_21`). The Docker build uses `eclipse-temurin:21-jdk-alpine`, which is the correct runtime target. The JDK 25 → JDK 21 comment in `build.gradle.kts` remains explanatory for local developers.

**Frontend port 3000.** The evaluator opens `http://localhost:3000`. This maps to nginx port 80 inside the container. Port 5173 is reserved for the Vite dev server and is intentionally not used for the Docker setup.

## Tradeoffs

- No Docker Compose `restart: unless-stopped` policy. The stack is for local dev evaluation, not production. Restart policies add noise for one-shot eval runs.
- No separate `docker-compose.override.yml` for dev vs. compose mode. Keeping a single file avoids complexity for a local-only setup.
- `frontend` `depends_on: backend` is a startup order hint only, not a health gate. The frontend is a static server that doesn't need the backend to be ready at container start — it just serves files; the browser makes API calls. Adding a backend health check would require an HTTP probe and extra complexity.

## Follow-ups

The full vertical slice is now self-contained and runnable from a single `docker compose up --build`. The system is ready for evaluation.
