<!--
Sync Impact Report
- Version change: none → 1.0.0 (initial ratification)
- Modified principles: n/a (template placeholders filled for the first time)
- Added sections: all 5 Core Principles, Technology Constraints, Development Workflow, Governance
- Removed sections: none
- Templates requiring updates:
  - .specify/templates/plan-template.md: ✅ no change needed (Constitution Check gate is already generic/pass-through)
  - .specify/templates/spec-template.md: ✅ no change needed
  - .specify/templates/tasks-template.md: ✅ no change needed
  - README.md / CLAUDE.md: ⚠ pending manual review (not touched by this run, scope was constitution only)
- Follow-up TODOs: none, no placeholder left undefined
-->

# Mon Home Ass Constitution

## Core Principles

### I. Readonly Data Boundary (NON-NEGOTIABLE)
The application MUST only read from the Home Assistant recorder database. No
write, update, delete, or DDL statement may ever be issued against it,
because the configured DB user has readonly permission only and the recorder
DB is owned and managed by Home Assistant itself, not by this app. Any
feature requiring persistence of app-owned data MUST use a separate store,
never the HA recorder DB.

### II. YAGNI / Minimal Diff
Every feature ships the smallest working slice: no speculative
abstractions, no config for values that never change, no pagination/caching/
search layered onto an endpoint before a real need for it is demonstrated
(see `user-roadmap.md` for the trigger-gated phases this applies to today).
Reuse the existing table → DTO → repo → route pattern already in the
codebase before inventing a new one. Rationale: every unused abstraction is a
future bug nobody asked for and a review nobody needed to do.

### III. Token-Efficient Execution (ponytail + caveman)
Every skill, agent, and command running in this project MUST operate under
`ponytail` (lazy-implementation discipline: YAGNI, stdlib/native first,
shortest working diff) and `caveman` (terse communication, no filler) for the
full duration of its execution, not just when explicitly invoked by the
user. Rationale: this project runs on a token budget; verbose exploration
and over-engineered solutions cost tokens twice — once to write, once to
review. Both modes stay active for code output and reasoning; git commits,
PRs, and security-relevant explanations are still written in full, normal
prose (per each mode's own boundaries) since clarity there is not optional.

### IV. Working App After Every Implementation (NON-NEGOTIABLE)
After every `speckit-implement` run, the application MUST build and run: at
minimum `./gradlew build` (or the equivalent compile+test task) passes, and
the server starts without crashing on startup. If a task leaves the app in a
broken state, the implementation is not done — fixing the build/startup
failure is part of that same implementation, not a follow-up task.
Rationale: a broken main branch blocks every subsequent phase and turns a
5-minute fix into a multi-phase archaeology problem later.

### V. Test-Backed Changes
New repositories, routes, and non-trivial utility logic (date/time
conversion, custom column types, filtering logic) MUST land with at least
one runnable test, following the existing `src/test/kotlin` structure
(route tests, repo/unit tests, `TestDatabase` fixture). Trivial one-liners
(simple getters, straight pass-through DTOs) do not need a dedicated test.
Rationale: matches the test coverage already established across
`EventsRoutesKtTest`, `RecorderRoutesKtTest`, `MigrationRoutesKtTest`, etc. —
consistency here is cheaper than re-deriving conventions per phase.

## Technology Constraints

Stack is fixed as: Kotlin + Ktor server, Exposed ORM against Postgres
(readonly recorder DB) with H2 for tests, kotlinx.serialization for DTOs,
`ktor-server-routing-openapi`/`swaggerUI` for the live API contract. New
dependencies require the same justification bar as any other addition under
Principle II — prefer what's already in `build.gradle.kts` before adding a
new library.

## Development Workflow

Follow the existing pattern per feature: table object (Exposed `Table`) →
DTO (`@Serializable` + `ResultRow.toXDto()`) → repo (plain class, no
interface) → route (`Route.xRoute(repo)` with `.describe { }` for the
OpenAPI/swagger contract) → wire into `configureRouting()` in `Routing.kt`.
`user-roadmap.md` and `openapi.yaml` are the standing design contract for
planned phases; keep them in sync with what actually ships, and don't
hand-maintain a third source of truth.

## Governance

This constitution supersedes ad-hoc practice for this repository. Amendments
require: a stated reason, a version bump per semantic rules (MAJOR = backward
incompatible principle removal/redefinition, MINOR = new/materially expanded
principle or section, PATCH = wording/clarification only), and an updated
Sync Impact Report at the top of this file. Any plan or task that violates a
principle above must either be changed to comply or carry an explicit,
written justification in that phase's own planning docs — silent violations
are not acceptable. Compliance is checked at the `Constitution Check` gate in
`plan-template.md` for every new feature plan.

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


**Version**: 1.0.0 | **Ratified**: 2026-07-30 | **Last Amended**: 2026-07-30
