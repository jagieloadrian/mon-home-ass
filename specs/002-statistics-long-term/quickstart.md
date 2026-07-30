# Quickstart: Long-term statistics for one entity

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).

## Validate

1. Find a known `statisticId` via Phase 1's endpoint:
   ```
   curl localhost:8080/statistics-meta | jq '.[0].statisticId'
   ```

2. Fetch its full history (US1):
   ```
   curl localhost:8080/statistics/<statisticId>
   ```
   Expect: 200, JSON array of `{startTs, mean, min, max, sum, state}` rows, oldest first.

3. Fetch a bounded range (US2), epoch seconds:
   ```
   curl "localhost:8080/statistics/<statisticId>?from=1700000000&to=1700100000"
   ```
   Expect: 200, only rows with `startTs` in range.

4. Same range, ISO-8601:
   ```
   curl "localhost:8080/statistics/<statisticId>?from=2023-11-14T00:00:00Z&to=2023-11-15T00:00:00Z"
   ```
   Expect: same shape as step 3.

5. Unknown entity (US3):
   ```
   curl -i localhost:8080/statistics/not-a-real-id
   ```
   Expect: 404 with `ErrorDto`.

6. Malformed range:
   ```
   curl -i "localhost:8080/statistics/<statisticId>?from=not-a-date"
   ```
   Expect: 400 with `ErrorDto`.

See [contracts/statistics-endpoint.yaml](./contracts/statistics-endpoint.yaml) for the full response schema and [data-model.md](./data-model.md) for field definitions.
