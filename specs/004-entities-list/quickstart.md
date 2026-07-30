# Quickstart: Entities list

## Prerequisites

- Local Postgres (or H2 for tests) with recorder schema loaded, `application.yaml` pointed at it.
- App built and running: `./gradlew run` (or `./gradlew build` then run the jar).

## Validate

1. Fetch the entity list:
   ```
   curl localhost:8080/entities
   ```
   Expect: 200, JSON array of `{id, entityId}` entries.

2. Empty case: point at a DB/schema with no `states_meta` rows.
   Expect: 200, `[]`.

3. DB down: stop the DB and repeat step 1.
   Expect: 503 with `ErrorDto`.

See [contracts/entities-endpoint.yaml](./contracts/entities-endpoint.yaml) and [data-model.md](./data-model.md) for details.
