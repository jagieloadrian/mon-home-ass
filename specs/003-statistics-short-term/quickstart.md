# Quickstart: Short-term (5-minute) statistics for one entity

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).

## Validate

1. Find a known `statisticId` via Phase 1's endpoint:
   ```
   curl localhost:8080/statistics-meta | jq '.[0].statisticId'
   ```

2. Fetch its full short-term history (US1):
   ```
   curl localhost:8080/statistics/<statisticId>/short-term
   ```
   Expect: 200, JSON array of `{startTs, mean, min, max, sum, state}` rows, oldest first — same shape as Phase 2's long-term endpoint.

3. Fetch a bounded range (US2):
   ```
   curl "localhost:8080/statistics/<statisticId>/short-term?from=1700000000&to=1700100000"
   ```
   Expect: 200, only rows with `startTs` in range.

4. Unknown entity (US3):
   ```
   curl -i localhost:8080/statistics/not-a-real-id/short-term
   ```
   Expect: 404 with `ErrorDto`.

5. Malformed range:
   ```
   curl -i "localhost:8080/statistics/<statisticId>/short-term?from=not-a-date"
   ```
   Expect: 400 with `ErrorDto`.

See [contracts/statistics-short-term-endpoint.yaml](./contracts/statistics-short-term-endpoint.yaml) and [data-model.md](./data-model.md) for details. Contract is identical to Phase 2's long-term endpoint except the path and backing table.
