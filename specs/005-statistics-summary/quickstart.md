# Quickstart: Statistics date-range summary

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).
- A known `statisticId` — get one from `curl localhost:8080/statistics-meta`.

## Validate

1. Full-history summary:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature/summary"
   ```
   Expect: 200, `{statisticId, from: null, to: null, min, max, avg, sum}`.

2. Bounded range with data:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature/summary?from=1700000000&to=1700003600"
   ```
   Expect: 200, aggregate fields reflect only rows in that range.

3. Bounded range with no matching rows:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature/summary?from=1&to=2"
   ```
   Expect: 200, `min`/`max`/`avg`/`sum` all `null`.

4. Unknown statisticId:
   ```
   curl localhost:8080/statistics/sensor.does_not_exist/summary
   ```
   Expect: 404 with `ErrorDto`.

5. Bad range value:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature/summary?from=not-a-date"
   ```
   Expect: 400 with `ErrorDto`.

6. DB down: stop the DB and repeat step 1.
   Expect: 503 with `ErrorDto`.

See [contracts/statistics-summary-endpoint.yaml](./contracts/statistics-summary-endpoint.yaml) and [data-model.md](./data-model.md) for details.
