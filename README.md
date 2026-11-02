# mon-home-ass

Readonly HTTP API over a Home Assistant recorder database. Exposes
statistics, entities, states, recorder/statistic runs, migrations, and
schema changes as JSON endpoints for external tools/dashboards, without
touching Home Assistant itself.

## Stack

Kotlin + Ktor server, Exposed ORM against the recorder DB — Postgres,
MariaDB, MySQL, or SQLite, selected via `ktor.database.database_type`
(Postgres/H2 in tests via Testcontainers) — Redis (Jedis) as a 5-minute
read-through cache, kotlinx.serialization, Ktor's built-in
OpenAPI/`swaggerUI` for the live API docs.

## Getting started

```bash
./gradlew run          # start the server (see src/main/resources/application.yaml for DB config)
./gradlew build         # compile + run tests
```

Swagger UI: `http://localhost:8080/swagger`

## Docs

- API contract — generated from route `.describe {}` blocks, not hand-maintained. Live at `/openapi.json` (raw) or `/swagger` (UI) on a running instance.
- `.devops/` — Flux HelmRelease (shared homelab-infra chart `charts/app`), see `.devops/README.md`
