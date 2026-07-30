---

description: "Task list template for feature implementation"
---

# Tasks: Short-term (5-minute) statistics for one entity

**Input**: Design documents from `/specs/003-statistics-short-term/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/statistics-short-term-endpoint.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new repos/routes ship with a test.

**Organization**: Tasks grouped by user story (spec.md P1/P2/P3). This feature extends
Phase 2's files (`StatisticRepo.kt`, `StatisticsRoutes.kt`) with a short-term
variant and reuses `StatisticDto` verbatim — no new DTO.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, DI/Routing/StatusPages plugins
already wired, `StatisticRepo` already provided via DI (Phase 2). Nothing to
do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Fix the pre-existing table-object bugs and add the repo/route skeleton all three stories extend.

- [X] T001 Fix `StatisticShortTem` in `src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`: (a) give the `Table()` constructor the explicit name `"statistics_short_term"`, (b) change `metadataId` to reference `StatisticMetaData.id` instead of `StateMetadata.id`
- [X] T002 Add `getShortTerm(statisticId: String, from: Instant?, to: Instant?): List<StatisticDto>?` to `src/main/kotlin/com/anjo/monhomeass/repository/StatisticRepo.kt` — identical logic to `getLongTerm` but joins `StatisticShortTem` to `StatisticMetaData` instead of `Statistic` (depends on T001)
- [X] T003 Add `statisticShortTermRoute(repo: StatisticRepo)` to `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`: `GET /statistics/{statisticId}/short-term`, same `from`/`to` parsing, 400/404 handling, and `.describe { }` block as `statisticRoute`, reusing the existing `parseInstant` helper (depends on T002)
- [X] T004 Wire `statisticShortTermRoute(statisticRepo)` into `configureRouting()` in `src/main/kotlin/com/anjo/monhomeass/plugins/Routing.kt` — reuses the `statisticRepo` binding already resolved for Phase 2's route (depends on T003)

**Checkpoint**: `GET /statistics/{statisticId}/short-term` compiles and serves — ready for story-level tests below.

---

## Phase 3: User Story 1 - Retrieve short-term statistics for a known entity (Priority: P1) 🎯 MVP

**Goal**: `GET /statistics/{statisticId}/short-term` returns all rows for a known entity when `from`/`to` are omitted.

**Independent Test**: `curl localhost:8080/statistics/<known-statisticId>/short-term` → 200, full row history.

- [X] T005 [US1] Add test in `src/test/kotlin/com/anjo/monhomeass/routing/StatisticShortTermRoutesKtTest.kt`: known `statisticId`, no query params → 200 + all expected rows, correct field mapping (nulls included)

**Checkpoint**: US1 fully functional and testable independently.

---

## Phase 4: User Story 2 - Filter by date range (Priority: P2)

**Goal**: `from`/`to` query params bound results by period start (epoch seconds or ISO-8601).

**Independent Test**: same endpoint with `from`/`to` set → only in-range rows returned.

- [X] T006 [US2] Add cases to `StatisticShortTermRoutesKtTest.kt`: `from` only, `to` only, both set (epoch seconds), both set (ISO-8601), `from` after `to` → `[]` (not an error) (depends on T005)
- [X] T007 [US2] Add malformed-value case: `from=not-a-date` → 400 + `ErrorDto` (depends on T003)

**Checkpoint**: US1 + US2 both work independently.

---

## Phase 5: User Story 3 - Unknown statistic returns a clear error (Priority: P3)

**Goal**: Unknown `statisticId` → 404, distinct from "known but empty".

**Independent Test**: `curl -i localhost:8080/statistics/not-a-real-id/short-term` → 404.

- [X] T008 [US3] Add cases to `StatisticShortTermRoutesKtTest.kt`: unknown `statisticId` → 404 + `ErrorDto`; known `statisticId` with zero rows in range → 200 + `[]` (not 404) (depends on T002)

**Checkpoint**: All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T009 Update `openapi.yaml` `/statistics/{statisticId}/short-term` to add the `400` response, per `contracts/statistics-short-term-endpoint.yaml` (200/404 already present) (depends on T003)
- [X] T010 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T009)
- [X] T011 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T010)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and T005 (extends the same test file).
- **User Story 3 (Phase 5)**: Depends on Phase 2; sequential with Phase 4, not parallel (same test file, edit conflicts).
- **Polish (Phase 6)**: Depends on all stories complete.

### Parallel Opportunities

- T001–T004 are sequential (each depends on the previous file/state existing).
- T006/T007/T008 all edit `StatisticShortTermRoutesKtTest.kt` — run sequentially despite different story labels.
- This phase's work (T001–T011) has no file overlap with Phase 2's already-shipped files except the three it explicitly extends — safe to work on independently of any other in-flight phase.

---

## Parallel Example: Phase 2

```bash
# T001 has no upstream dependency and can start immediately.
Task: "Fix StatisticShortTem in src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (includes the table-object bugfix)
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl` a known statisticId's short-term endpoint, confirm full history returns
4. Ship — US2/US3 are additive refinements, not blockers to a usable endpoint

### Incremental Delivery

1. Foundational → table fixed, endpoint exists, unfiltered
2. + US1 test → confirmed correct for the base case (MVP)
3. + US2 → date-range filtering confirmed
4. + US3 → 404 behavior confirmed
5. + Polish → openapi.yaml sync, full build, quickstart validation
