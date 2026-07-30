# Quickstart: Statistic Entities List

## Automated (preferred)

```bash
./gradlew test --tests "*StatisticsMetaRoutesKtTest*"
```

Covers: happy path (rows → DTOs), empty DB (empty array, not error), and
DB-failure → 503 (FR-006). See [contracts/statistics-meta.md](./contracts/statistics-meta.md)
for the exact response shape.

## Manual, against a running app

Prereq: `src/main/resources/application.yaml` points at a reachable Postgres
recorder DB (or run against H2 with `application-test.yaml`, same as the
test suite).

```bash
./gradlew run
curl -s http://localhost:8080/statistics-meta | jq .
```

Expected: JSON array, one object per row in `statistics_meta`, matching
[data-model.md](./data-model.md). Empty DB → `[]`.

To see the 503 path manually: point `application.yaml` at an unreachable
host/port, restart, and repeat the `curl` — expect HTTP 503 with a JSON
`{"message": "..."}` body instead of a bare connection-refused/500.

Swagger UI (existing, auto-registers the new route once shipped):
`http://localhost:8080/swagger`
