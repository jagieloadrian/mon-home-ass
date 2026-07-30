# mon-home-ass

Readonly HTTP API over a Home Assistant recorder database. Exposes
statistics, entities, recorder runs, migrations, and schema changes as JSON
endpoints for external tools/dashboards, without touching Home Assistant
itself.

## Stack

Kotlin + Ktor server, Exposed ORM (Postgres in prod, H2 in tests),
kotlinx.serialization, `ktor-server-routing-openapi` + `swaggerUI` for the
live API docs.

## Rules

This project follows `.specify/memory/constitution.md`. Short version:
- The DB user is readonly — the app never writes to the recorder DB.
- New features follow the existing pattern: table → DTO → repo → route.
- No pagination/caching/search until a real need shows up — see
  `user-roadmap.md` for what's planned and what's intentionally deferred.
- Every `speckit-implement` run must leave the app building and starting.

## Getting started

```bash
./gradlew run          # start the server (see src/main/resources/application.yaml for DB config)
./gradlew build         # compile + run tests
```

Swagger UI: `http://localhost:8080/swagger`

## Docs

- `openapi.yaml` — API contract
- `user-roadmap.md` — phased feature plan
- `notatki.txt` — source Home Assistant recorder DB schema
- `.devops/` — Kubernetes manifests + Helm chart (initial scaffold)
