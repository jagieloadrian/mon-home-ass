    ---

description: "Task list template for feature implementation"
---

# Tasks: Statistics date-range summary

**Input**: Design documents from `/specs/005-statistics-summary/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/statistics-summary-endpoint.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new repo methods and routes ship with a test.

**Organization**: Single user story (spec.md has only P1) — the entire feature is
one story, so Foundational and User Story 1 collapse into one build-then-test
sequence, following the `statistics-long-term` (Phase 2) file pattern.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, `Statistic`/`StatisticMetaData`
tables already modeled, DI/Routing/StatusPages plugins already wired,
`StatisticRepo` already exists and provided via DI. Nothing to do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DTO + repo method + route — the entire feature, since there is only one user story.

- [X] T001 [P] Create `StatisticSummaryDto` (`statisticId: String`, `from: String?`, `to: String?`, `min: Double?`, `max: Double?`, `avg: Double?`, `sum: Double?`) in `src/main/kotlin/com/anjo/monhomeass/model/dto/StatisticSummaryDto.kt`
- [X] T002 Add `getSummary(statisticId: String, from: Instant?, to: Instant?): StatisticSummaryDto?` to `src/main/kotlin/com/anjo/monhomeass/repository/StatisticRepo.kt` — reuse the existing `StatisticMetaData` existence check (return `null` if unknown), then join `Statistic`/`StatisticMetaData` filtered by `statisticId`/`from`/`to` and select `MIN(Statistic.state)`, `MAX(Statistic.state)`, `AVG(Statistic.state)`, `SUM(Statistic.state)` as a single aggregate row (depends on T001)
- [X] T003 Add `statisticSummaryRoute(repo: StatisticRepo)` to `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`: `GET /statistics/{statisticId}/summary`, reusing `parseInstant` for `from`/`to` (400 on unparseable), 404 when `repo.getSummary` returns `null`, `.describe { }` block per existing `statisticRoute` pattern (200/400/404/503 responses) (depends on T002)
- [X] T004 Wire `statisticSummaryRoute(statisticRepo)` into `configureRouting()` in `src/main/kotlin/com/anjo/monhomeass/plugins/Routing.kt` (depends on T003) — no DI change needed, `StatisticRepo` already provided

**Checkpoint**: `GET /statistics/{statisticId}/summary` compiles and serves.

---

## Phase 3: User Story 1 - Get aggregate stats for a date range (Priority: P1) 🎯 MVP

**Goal**: `GET /statistics/{statisticId}/summary` returns one aggregate row for a known entity, optionally bounded by `from`/`to`.

**Independent Test**: `curl "localhost:8080/statistics/{statisticId}/summary?from=...&to=..."` → 200, one object with min/max/avg/sum.

- [X] T005 [US1] Add `StatisticSummaryRoutesKtTest.kt` in `src/test/kotlin/com/anjo/monhomeass/routing/` (existing tests are split per route group, no single `StatisticsRoutesKtTest.kt` — follows that pattern): happy path (seeded rows → 200 + expected aggregate), ranged (200 + aggregate over in-range rows only), empty-range (200 with all-null aggregate fields), unknown `statisticId` (404), bad `from` (400), and DB-down (503 + `ErrorDto`)

**Checkpoint**: Feature fully functional and testable — this is the whole feature.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T006 Fix `openapi.yaml` per `contracts/statistics-summary-endpoint.yaml`: add `400`/`503` responses and a body schema on `404` to `/statistics/{statisticId}/summary`, mark `StatisticSummaryDto.from`/`to` as `nullable: true`
- [X] T007 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T006)
- [X] T008 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. Delivers the whole feature (single user story).
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **Polish (Phase 4)**: Depends on Phase 3 complete.

### Parallel Opportunities

- T001 has no dependency and can start immediately; T002–T004 are sequential (each depends on the previous file existing).
- This phase touches only one new file (`StatisticSummaryDto.kt`) plus additive edits to shared files (`StatisticRepo.kt`, `StatisticsRoutes.kt`, `Routing.kt`) — no overlap with any other in-flight phase's new files.

---

## Parallel Example: Phase 2

```bash
# T001 can start immediately; T002 waits on it.
Task: "Create StatisticSummaryDto in src/main/kotlin/com/anjo/monhomeass/model/dto/StatisticSummaryDto.kt"
```

---

## Implementation Strategy

### MVP First (and only)

1. Complete Phase 2: Foundational
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl "localhost:8080/statistics/{statisticId}/summary"`, confirm aggregate returns
4. Complete Phase 4: Polish (openapi sync, build, quickstart)
5. Ship — this is the entire feature, no incremental stories to add later
