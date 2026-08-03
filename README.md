# mon-home-ass

Readonly HTTP API over a Home Assistant recorder database. Exposes
statistics, entities, recorder runs, migrations, and schema changes as JSON
endpoints for external tools/dashboards, without touching Home Assistant
itself.

## Stack

Kotlin + Ktor server, Exposed ORM (Postgres in prod and in tests via
Testcontainers), Redis (Jedis) as a 5-minute read-through cache,
kotlinx.serialization, Ktor's built-in OpenAPI/`swaggerUI` for the live API
docs.

## Rules

This project follows `.specify/memory/constitution.md`. Short version:
- The DB user is readonly — the app never writes to the recorder DB.
- New features follow the existing pattern: table → DTO → repo → route.
- Every `speckit-implement` run must leave the app building and starting.

## Historia rozwoju

- **Statistics meta list**: `GET /statistics-meta` — pełna lista śledzonych
  statystyk z recorder DB (id, statistic_id, nazwa, jednostka, źródło, flagi
  mean/sum). Pusta baza → `[]`, awaria bazy → 503.
- **Statistics long term**: `GET /statistics/{statisticId}` — historyczne
  próbki (mean/min/max/sum/state per okres), opcjonalny zakres `from`/`to`
  (epoch sekundy lub ISO-8601). Nieznany `statisticId` → 404, zły format →
  400, brak wierszy w zakresie → `[]`.
- **Statistics short term**: `GET /statistics/{statisticId}/short-term` —
  dane 5-minutowe, ten sam kształt i te same reguły co long-term.
- **Entities list**: `GET /entities` — lista znanych encji Home Assistant
  (`entity_id`).
- **Statistics summary**: `GET /statistics/{statisticId}/summary` — agregaty
  min/max/avg/sum (kolumna `state`) liczone w SQL dla zakresu dat.
- **Statistics meta search**: `?search=` na `/statistics-meta` — filtr
  podciągu po `statistic_id`/`name`, bez rozróżniania wielkości liter, w SQL.
- **Statistics pagination**: `limit`/`offset` na endpointach long/short-term,
  realizowane przez SQL `LIMIT`/`OFFSET`.
- **Read caching**: warstwa cache w Redis przed wszystkimi endpointami
  odczytu, TTL 5 minut, klucz per pełny zestaw parametrów zapytania. Błędy
  (400/404/503) nie są cache'owane.
- **Dependency updates**: Kotlin, Ktor, Exposed, driver PostgreSQL, H2,
  Logback podbite do aktualnych stabilnych wersji; zero zmian zachowania API.
- **CI build check & deploy scaffold**: GitHub Actions workflow (build +
  test + próg pokrycia JaCoCo/Codecov, min. 70%) na push do `main` i na PR;
  gotowe manifesty Kubernetes + Helm chart w `.devops/` (bez commitowanych
  sekretów).

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
