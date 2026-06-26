# Report 002 — Backend Foundation

## Summary

The Itinera backend is now a runnable Spring Boot service. It connects to PostgreSQL, runs Flyway migrations on startup, and exposes a live health endpoint. The project has a working test execution path. From this point forward, every implementation slice has a backend to land in.

## Deliverables

- **Spring Boot application** — `ItineraApplication.kt` bootstrapped with `@SpringBootApplication`, running on port 8080 via embedded Tomcat.
- **Gradle build** — Kotlin DSL (`build.gradle.kts`), Gradle 9.6.1 wrapper, dependencies: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-jdbc`, `flyway-core`, `flyway-database-postgresql`, `postgresql`.
- **Database connectivity** — HikariCP connection pool wired to `itinera` PostgreSQL instance from `docker-compose.yml`.
- **Flyway migration** — `V1__initial_backend_foundation.sql` creates `app_schema_version_marker` and inserts a marker row; applied on first startup.
- **Health endpoint** — `GET /actuator/health` returns `{"status":"UP"}`.
- **Context load test** — `ItineraApplicationTests.contextLoads()` starts the full Spring context against the real database.
- **`backend/README.md`** — updated with run, test, and single-test commands.

## Validation

**Tests:**
```
./gradlew test  →  BUILD SUCCESSFUL
ItineraApplicationTests > contextLoads() PASSED
```

**Runtime:**
```
./gradlew bootRun  →  Started ItineraApplicationKt in 2.9s
Flyway: Successfully validated 1 migration
Flyway: Current version of schema "public": 1 (up to date)
curl http://localhost:8080/actuator/health  →  {"status":"UP"}
```

## Tests

`ItineraApplicationTests.contextLoads()` runs a full `@SpringBootTest` against the local PostgreSQL instance. The test verifies that Spring context wiring (datasource, Flyway, actuator) is correct. The test database is the same instance used for local development — a real PostgreSQL connection is required to run tests.

## Engineering Notes

**Java 25 / Kotlin 1.9.x incompatibility.** The development machine runs Java 25, which Kotlin 1.9.25's internal Java version parser cannot handle (it parses the version string `"25.0.2"` as malformed). The fix was to upgrade the Kotlin Gradle plugin to **2.1.21**, which resolves this through the K2 compiler pipeline. The JVM bytecode target is pinned to **21** in both the Kotlin and Java compile tasks so the compiled output remains compatible with Java 21 runtimes even though the build runs on Java 25.

**Gradle 8.14.5 does not support Java 25.** The Spring Initializr-generated wrapper used Gradle 8.14.5. Upgraded to **Gradle 9.6.1** (current release), which resolved the daemon compatibility issue.

**`spring-boot-starter-web` is required for HTTP actuator.** The generated project included `spring-boot-starter-actuator` but not `spring-boot-starter-web`. Without an embedded servlet container, Spring Boot starts and immediately exits; actuator endpoints have no HTTP transport to bind to. Added `spring-boot-starter-web` to provide the embedded Tomcat server.

## Tradeoffs

The context load test connects to a live PostgreSQL instance rather than an in-memory or embedded database. This was a deliberate choice: the project uses PostgreSQL-specific features (JSONB, Flyway migrations with PostgreSQL dialect) that in-memory databases cannot replicate. The tradeoff is that `./gradlew test` requires Docker to be running. This is documented in `backend/README.md`.

The `application.yml` uses hardcoded local credentials. No environment profiles or configuration injection were introduced at this stage — the task explicitly asked for "sane defaults." A future slice can add profile-based configuration when deployment targets require it (tracked in `docs/FUTURE_FORWARDS.md`).

## Follow-ups

Next: **Phase 2 — Persistence Model** from `docs/PLAN.md`.

This covers the four core Flyway migrations (`onboarding_session`, `onboarding_step_state`, `provider_validation_attempt`, `partner_account`), the JSONB column definitions, and the Kotlin repository layer using `NamedParameterJdbcTemplate`. This is the schema the rest of the onboarding workflow will be built on.
