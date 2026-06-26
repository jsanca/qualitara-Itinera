# Task 002 — Backend Foundation

## Context

Project: Itinera

Phase 1 establishes the backend runtime foundation. This task should create a runnable Kotlin Spring Boot backend connected to PostgreSQL through Flyway migrations.

Do not implement onboarding business logic yet.

## Goal

Create the Spring Boot Kotlin backend scaffold with:

* Gradle build
* PostgreSQL configuration
* Flyway migration setup
* Health endpoint
* Basic test execution

## Scope

Implement inside `backend/`.

Expected capabilities:

* Backend starts locally.
* Backend connects to PostgreSQL from `docker-compose.yml`.
* Flyway runs on startup.
* Health endpoint responds.
* Tests can run.

## Required Stack

Use:

* Kotlin
* Spring Boot
* Gradle Kotlin DSL
* Flyway
* PostgreSQL driver
* Spring JDBC
* Spring Boot Actuator
* JUnit 5

Do not use JPA.

## Expected Files

Create or update:

```text
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle/
├── src/main/kotlin/...
├── src/main/resources/application.yml
├── src/main/resources/db/migration/V1__initial_backend_foundation.sql
└── src/test/kotlin/...
```

## Health Endpoint

Use Spring Boot Actuator health.

Expected endpoint:

```text
GET /actuator/health
```

Expected response should indicate the app is up.

## Flyway

Add a minimal first migration that proves Flyway is wired correctly.

Do not create the full onboarding schema yet.

Example:

```sql
create table app_schema_version_marker (
    id integer primary key,
    description text not null
);

insert into app_schema_version_marker (id, description)
values (1, 'backend foundation initialized');
```

The real onboarding schema will be added in Phase 2.

## Configuration

Configure the backend to connect to local PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/itinera
    username: itinera
    password: itinera
  flyway:
    enabled: true
```

Use sane defaults. Do not introduce complex environment profiles unless necessary.

## Tests

Add a minimal test proving the Spring context loads.

If practical, add a simple health/controller smoke test.

Do not add business tests yet.

## Documentation Updates

Update:

* `backend/README.md`
* `AI_LOG.md`
* `docs/agents/reports/002-backend-foundation.md`
* `docs/FUTURE_FORWARDS.md` if needed

Add this task as:

```text
docs/agents/tasks/002-backend-foundation.md
```

## Report Requirements

The report must follow the standardized report structure:

* Summary
* Deliverables
* Validation
* Tests
* Engineering Notes
* Tradeoffs
* Follow-ups

The report should describe the engineering capability added:

> Backend runtime established.

## Validation Commands

Run and report results:

```bash
docker compose up -d postgres
cd backend
./gradlew test
./gradlew bootRun
```

Then verify:

```bash
curl http://localhost:8080/actuator/health
```

Also confirm Flyway migration ran successfully.

## Constraints

* Do not implement onboarding sessions.
* Do not implement provider validation.
* Do not implement REST onboarding APIs.
* Do not create the full database schema yet.
* Do not add JPA.
* Do not add frontend code.
* Do not add backend/frontend services to Docker Compose yet.

## Success Criteria

* Backend project exists.
* Backend starts.
* Backend connects to PostgreSQL.
* Flyway runs.
* Health endpoint works.
* Tests execute.
* Documentation and AI log index are updated.
