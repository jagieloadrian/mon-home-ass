---

description: "Task list template for feature implementation"
---

# Tasks: Search filter on statistics-meta

**Input**: Design documents from `/specs/006-statistics-meta-search/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/statistics-meta-search-endpoint.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new/changed query logic ship with a test.

**Organization**: Single user story (spec.md has only P1) — the entire feature is
one story, so Foundational and User Story 1 collapse into one build-then-test
sequence. No new files — this extends the existing `statistics-meta` (Phase 1) code.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, `StatisticMetaData` table
already modeled, `StatisticMetaRepo`/`statisticMetaRoute` already exist and
wired. Nothing to do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend the existing repo method and route — the entire feature, since there is only one user story.

- [X] T001 Add optional `search: String? = null` parameter to `StatisticMetaRepo.getAll()` in `src/main/kotlin/com/anjo/monhomeass/repository/StatisticMetaRepo.kt` — when `search` is non-blank, filter with `StatisticMetaData.statisticId.lowerCase() like "%${search.lowercase()}%"` OR `StatisticMetaData.name.lowerCase() like "%${search.lowercase()}%"`; when `search` is `null` or blank, behave exactly as today (no `where` clause)
- [X] T002 Read the `search` query parameter in `statisticMetaRoute` in `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt` and pass it to `repo.getAll(search)`; add a `query("search")` block (`required = false`) to the existing `.describe { }` responses (depends on T001)

**Checkpoint**: `GET /statistics-meta?search=...` compiles and serves.

---

## Phase 3: User Story 1 - Find a statistic by name or id substring (Priority: P1) 🎯 MVP

**Goal**: `GET /statistics-meta?search=X` returns only entries whose `statistic_id` or `name` contains `X`, case-insensitively; omitting/blank `search` is unchanged.

**Independent Test**: `curl "localhost:8080/statistics-meta?search=temperature"` → 200, only matching entries.

- [X] T003 [US1] Add search test cases to `src/test/kotlin/com/anjo/monhomeass/routing/StatisticsMetaRoutesKtTest.kt`: matching substring in `statistic_id` (200, filtered), matching substring in `name` different case (200, filtered — proves case-insensitivity), non-matching substring (200, empty array), and absent/blank `search` (200, full list unchanged — regression guard on FR-003)

**Checkpoint**: Feature fully functional and testable — this is the whole feature.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T004 Add the `search` query parameter to `/statistics-meta` in `openapi.yaml` per `contracts/statistics-meta-search-endpoint.yaml`
- [X] T005 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T004)
- [X] T006 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T005)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. Delivers the whole feature (single user story).
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **Polish (Phase 4)**: Depends on Phase 3 complete.

### Parallel Opportunities

- None — T001/T002 touch the same feature's tightly coupled repo+route pair, and every task after T001 depends on it. This is the smallest phase yet (no new files).

---

## Implementation Strategy

### MVP First (and only)

1. Complete Phase 2: Foundational
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl "localhost:8080/statistics-meta?search=..."`, confirm filtering works and no-param case is unchanged
4. Complete Phase 4: Polish (openapi sync, build, quickstart)
5. Ship — this is the entire feature, no incremental stories to add later
