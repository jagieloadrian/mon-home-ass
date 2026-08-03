# Quickstart: Pagination on long/short-term statistics

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).
- A known `statisticId` with more than a couple rows — get one from `curl localhost:8080/statistics-meta`.

## Validate

1. No `limit`/`offset` — unchanged behavior:
   ```
   curl localhost:8080/statistics/sensor.living_room_temperature
   ```
   Expect: 200, every matching row.

2. First page:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature?limit=1"
   ```
   Expect: 200, exactly 1 row (the oldest).

3. Next page:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature?limit=1&offset=1"
   ```
   Expect: 200, the row after the first one.

4. Offset beyond available rows:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature?offset=9999"
   ```
   Expect: 200, `[]`.

5. Negative value:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature?limit=-1"
   ```
   Expect: 400 with `ErrorDto`.

6. Short-term variant — same behavior:
   ```
   curl "localhost:8080/statistics/sensor.living_room_temperature/short-term?limit=1"
   ```
   Expect: 200, exactly 1 row.

See [contracts/statistics-pagination-endpoints.yaml](./contracts/statistics-pagination-endpoints.yaml) and [data-model.md](./data-model.md) for details.
