# Quickstart: Search filter on statistics-meta

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).

## Validate

1. No `search` param — unchanged behavior:
   ```
   curl localhost:8080/statistics-meta
   ```
   Expect: 200, every entry.

2. Matching substring in `statistic_id` or `name` (any case):
   ```
   curl "localhost:8080/statistics-meta?search=temperature"
   ```
   Expect: 200, only entries containing "temperature" (any case) in
   `statistic_id` or `name`.

3. Non-matching substring:
   ```
   curl "localhost:8080/statistics-meta?search=nope-does-not-exist"
   ```
   Expect: 200, `[]`.

4. Empty `search` value — same as no param:
   ```
   curl "localhost:8080/statistics-meta?search="
   ```
   Expect: 200, every entry.

See [contracts/statistics-meta-search-endpoint.yaml](./contracts/statistics-meta-search-endpoint.yaml) and [data-model.md](./data-model.md) for details.
