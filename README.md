# mon-home-ass

Readonly HTTP API over a Home Assistant recorder database. Exposes
statistics, entities, recorder runs, migrations, and schema changes as JSON
endpoints for external tools/dashboards, without touching Home Assistant
itself.

## Stack

Kotlin + Ktor server, Exposed ORM (Postgres in prod and in tests via
Testcontainers), Redis (Jedis) as a 5-minute read-through cache,
kotlinx.serialization, Ktor's built-in OpenAPI/`swaggerUI` for the live API
docs.

## Rules

This project follows `.specify/memory/constitution.md`. Short version:
- The DB user is readonly — the app never writes to the recorder DB.
- New features follow the existing pattern: table → DTO → repo → route.
- Every `speckit-implement` run must leave the app building and starting.

## Getting started

```bash
./gradlew run          # start the server (see src/main/resources/application.yaml for DB config)
./gradlew build         # compile + run tests
```

Swagger UI: `http://localhost:8080/swagger`

## Docs

- `openapi.yaml` — API contract
- `.devops/` — Kubernetes manifests + Helm chart (initial scaffold)
