# backend

Kotlin Spring Boot API for Itinera.

## Running

Start PostgreSQL first:

```bash
docker compose up -d postgres
```

Run the backend:

```bash
./gradlew bootRun
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Testing

```bash
./gradlew test
```

Run a single test class:

```bash
./gradlew test --tests "com.qualitara.itinera.ItineraApplicationTests"
```

## Stack

- Kotlin 2.1.21 + Spring Boot 3.5.0
- Gradle 9.6.1 (Kotlin DSL)
- PostgreSQL 16 via HikariCP
- Flyway migrations: `src/main/resources/db/migration/`
- Spring JDBC (`NamedParameterJdbcTemplate`)
- Spring Boot Actuator (`/actuator/health`)

See `docs/ARCHITECTURE.md` for design details.
