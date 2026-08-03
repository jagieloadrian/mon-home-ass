---

description: "Task list template for feature implementation"
---

# Tasks: Pagination on long/short-term statistics

**Input**: Design documents from `/specs/007-statistics-pagination/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/statistics-pagination-endpoints.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new/changed query logic ship with a test.

**Organization**: Single user story (spec.md has only P1) — the entire feature is
one story, so Foundational and User Story 1 collapse into one build-then-test
sequence. No new files — this extends the existing long/short-term statistics
(Phase 2/3) code.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, `Statistic`/`StatisticShortTem`/
`StatisticMetaData` tables already modeled, `StatisticRepo` and both routes
already exist and wired. Nothing to do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend the existing repo methods and routes — the entire feature, since there is only one user story.

- [X] T001 Add optional `limit: Int? = null, offset: Long? = null` parameters to `StatisticRepo.getLongTerm()` in `src/main/kotlin/com/anjo/monhomeass/repository/StatisticRepo.kt` — after the existing `.orderBy(Statistic.startTs, SortOrder.ASC)`, append `.limit(limit)` when `limit != null` and `.offset(offset)` when `offset != null`
- [X] T002 [P] Add the same optional `limit: Int? = null, offset: Long? = null` parameters to `StatisticRepo.getShortTerm()` in `src/main/kotlin/com/anjo/monhomeass/repository/StatisticRepo.kt`, identical shape to T001 applied to `StatisticShortTem.startTs` ordering
- [X] T003 Read and validate `limit`/`offset` query params in `statisticRoute` in `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`: parse as `Int`/`Long`, 400 (`ErrorDto`) if present but non-numeric or negative; pass parsed values to `repo.getLongTerm(statisticId, from, to, limit, offset)`; add `query("limit")`/`query("offset")` blocks (`required = false`) to the existing `.describe { }` responses (depends on T001)
- [X] T004 [P] Apply the same query-param read/validate/wire change to `statisticShortTermRoute` in `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`, calling `repo.getShortTerm(statisticId, from, to, limit, offset)` (depends on T002)

**Checkpoint**: `GET /statistics/{statisticId}` and its `/short-term` variant both compile and serve with `limit`/`offset`.

---

## Phase 3: User Story 1 - Fetch one page of a large statistic range (Priority: P1) 🎯 MVP

**Goal**: Both endpoints return at most `limit` rows starting after `offset`, oldest-first, applied after any `from`/`to` filter; omitting both is unchanged; negative/non-numeric values are 400; offset past the end is an empty 200.

**Independent Test**: `curl "localhost:8080/statistics/{statisticId}?limit=1&offset=1"` → 200, exactly the second-oldest row.

- [X] T005 [US1] Add pagination test cases to `src/test/kotlin/com/anjo/monhomeass/routing/StatisticLongTermRoutesKtTest.kt`: `limit` alone (200, first N rows), `limit`+`offset` (200, next page), `offset` beyond total rows (200, empty array), negative `limit` (400), absent `limit`/`offset` (200, full list — regression guard on FR-002)
- [X] T006 [P] [US1] Add the equivalent pagination test cases to `src/test/kotlin/com/anjo/monhomeass/routing/StatisticShortTermRoutesKtTest.kt`, same shape as T005 applied to the short-term endpoint (per FR-008)

**Checkpoint**: Feature fully functional and testable — this is the whole feature.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T007 Add `limit`/`offset` query params to both `/statistics/{statisticId}` and `/statistics/{statisticId}/short-term` in `openapi.yaml` per `contracts/statistics-pagination-endpoints.yaml`
- [X] T008 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T007)
- [X] T009 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. Delivers the whole feature (single user story).
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **Polish (Phase 4)**: Depends on Phase 3 complete.

### Parallel Opportunities

- T001 (long-term repo) and T002 (short-term repo) touch the same file but disjoint methods — safe to do together, marked [P].
- T003 depends on T001; T004 depends on T002 — both edit `StatisticsRoutes.kt` but disjoint functions, safe in parallel once their respective repo task lands.
- T005 and T006 are separate test files — [P].

---

## Parallel Example: Phase 2

```bash
# T001 and T002 touch disjoint methods in the same repo file — safe together.
Task: "Add limit/offset to StatisticRepo.getLongTerm()"
Task: "Add limit/offset to StatisticRepo.getShortTerm()"
```

---

## Implementation Strategy

### MVP First (and only)

1. Complete Phase 2: Foundational
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl "localhost:8080/statistics/{statisticId}?limit=1&offset=1"`, confirm both endpoints paginate and no-param case is unchanged
4. Complete Phase 4: Polish (openapi sync, build, quickstart)
5. Ship — this is the entire feature, no incremental stories to add later
