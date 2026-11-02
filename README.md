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

Start the server on `http://localhost:8080`. The database comes from `ktor.database` in
`src/main/resources/application.yaml`, set through `DB_TYPE` (`postgres`, `mariadb`, `mysql`, `sqlite`), `DB_URL`
(`host:port`), `DB_NAME`, `DB_USER`, `DB_PASSWORD` and `DB_DRIVER`; `sqlite` reads the file at `DB_FILE_PATH`
instead:

```bash
./gradlew run
```

Compile and run the tests (Testcontainers, so Docker must be running):

```bash
./gradlew build
```

Swagger UI: `http://localhost:8080/swagger`

Cross-origin browser calls are allowed only from the hosts in `CORS_HOSTS`: comma-separated, without scheme (`http`
and `https` both match), e.g. `statistics.home.arpa,localhost:8081`; default `localhost:8080`, empty = no
cross-origin access.

Health probes: `/health/ready` answers 503 while the database is unreachable, `/health/live` stays 200.

The statistics publisher (NATS JetStream) starts only when `NATS_URL` is set; the homelab NATS is not wired yet,
see `.devops/README.md`.

## Docs

- API contract — generated from route `.describe {}` blocks, not hand-maintained. Live at `/openapi.json` (raw) or `/swagger` (UI) on a running instance.
- `.devops/` — Flux HelmRelease (shared homelab-infra chart `charts/app`), see `.devops/README.md`
