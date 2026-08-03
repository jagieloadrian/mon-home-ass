# Quickstart: Read caching layer

## Prerequisites

- Local Postgres with recorder schema loaded, and a reachable Redis
  instance — `application.yaml` pointed at both (`database.*` and
  `redis.*` sections).
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).
- Verify Redis is actually being hit: `redis-cli -h <host> -p <port> KEYS '*'`
  should show `statistics-meta:*`/`entities:all`/etc. keys after step 1.

## Validate

1. Baseline call — populates the cache:
   ```
   curl localhost:8080/statistics-meta
   ```
   Expect: 200, current data.

2. Change the underlying row directly in Postgres (bypassing the app —
   e.g. `UPDATE statistics_meta SET name = 'changed' WHERE data_id = 1;`),
   then immediately repeat step 1 within 5 minutes:
   ```
   curl localhost:8080/statistics-meta
   ```
   Expect: 200, **still the old value** — proves the response came from
   cache, not a fresh query (FR-002/FR-003).

3. Wait 5+ minutes (or use the same steps against `/statistics/{statisticId}`,
   `/statistics/{statisticId}/short-term`, `/statistics/{statisticId}/summary`,
   or `/entities` — all five endpoints are in scope), then repeat step 1
   again:
   ```
   curl localhost:8080/statistics-meta
   ```
   Expect: 200, the **new** value — cache expired, fresh query ran.

4. Two different requests stay isolated:
   ```
   curl "localhost:8080/statistics-meta?search=temperature"
   curl "localhost:8080/statistics-meta?search=energy"
   ```
   Expect: each returns its own filtered result — neither serves the
   other's cached response.

See [data-model.md](./data-model.md) and [research.md](./research.md) for
the cache mechanism and key-strategy decisions.
