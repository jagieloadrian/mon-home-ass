---

description: "Task list template for feature implementation"
---

# Tasks: Long-term statistics for one entity

**Input**: Design documents from `/specs/002-statistics-long-term/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/statistics-endpoint.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new repos/routes ship with a test.

**Organization**: Tasks grouped by user story (spec.md P1/P2/P3). This feature is a single
endpoint, so all stories touch the same three files (`StatisticDto.kt`,
`StatisticRepo.kt`, `StatisticsRoutes.kt`) — each story phase is a real
incremental capability added to those files, not a separate file set.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, `Statistic`/`StatisticMetaData`
tables already modeled, DI/Routing/StatusPages plugins already wired
(Phase 1 `statistics-meta`). Nothing to do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DTO + repo/route skeleton all three stories extend.

- [X] T001 [P] Create `StatisticDto` (`startTs: Long`, `mean`/`min`/`max`/`sum`/`state: Double?`) and `ResultRow.toStatisticDto()` mapper in `src/main/kotlin/com/anjo/monhomeass/model/dto/StatisticDto.kt`
- [X] T002 Create `StatisticRepo` in `src/main/kotlin/com/anjo/monhomeass/repository/StatisticRepo.kt` with `getLongTerm(statisticId: String, from: Instant?, to: Instant?): List<StatisticDto>?` — returns `null` when `statisticId` doesn't resolve in `StatisticMetaData` (route maps `null` → 404); otherwise joins `Statistic` to `StatisticMetaData` on `metadataId`/`id`, filters by `statisticId`, applies optional `from`/`to` bounds on `startTs`, orders by `startTs` ascending (depends on T001)
- [X] T003 Add `statisticRoute(repo: StatisticRepo)` to `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`: `GET /statistics/{statisticId}`, parses `from`/`to` query params (epoch-seconds `Long` or ISO-8601 via `kotlin.time.Instant.parse`, whichever succeeds), 400 on unparseable value, 404 when repo returns `null`, `.describe { }` block per existing `statisticMetaRoute` pattern (depends on T002)
- [X] T004 Provide `StatisticRepo` in `src/main/kotlin/com/anjo/monhomeass/plugins/DependencyInjection.kt` (depends on T002)
- [X] T005 Wire `statisticRoute(statisticRepo)` into `configureRouting()` in `src/main/kotlin/com/anjo/monhomeass/plugins/Routing.kt` (depends on T003, T004)

**Checkpoint**: `GET /statistics/{statisticId}` compiles and serves — ready for story-level tests below.

---

## Phase 3: User Story 1 - Retrieve long-term statistics for a known entity (Priority: P1) 🎯 MVP

**Goal**: `GET /statistics/{statisticId}` returns all rows for a known entity when `from`/`to` are omitted.

**Independent Test**: `curl localhost:8080/statistics/<known-statisticId>` → 200, full row history.

- [X] T006 [US1] Add `RecorderDb`-seeded test in `src/test/kotlin/com/anjo/monhomeass/routing/StatisticLongTermRoutesKtTest.kt`: known `statisticId`, no query params → 200 + all expected rows, correct field mapping (nulls included)

**Checkpoint**: US1 fully functional and testable independently.

---

## Phase 4: User Story 2 - Filter by date range (Priority: P2)

**Goal**: `from`/`to` query params bound results by period start (epoch seconds or ISO-8601).

**Independent Test**: same endpoint with `from`/`to` set → only in-range rows returned.

- [X] T007 [US2] Add cases to `StatisticLongTermRoutesKtTest.kt`: `from` only, `to` only, both set (epoch seconds), both set (ISO-8601), `from` after `to` → `[]` (not an error) — assert only in-range rows returned in each case (depends on T006)
- [X] T008 [US2] Add malformed-value case: `from=not-a-date` → 400 + `ErrorDto` (depends on T003)

**Checkpoint**: US1 + US2 both work independently.

---

## Phase 5: User Story 3 - Unknown statistic returns a clear error (Priority: P3)

**Goal**: Unknown `statisticId` → 404, distinct from "known but empty".

**Independent Test**: `curl -i localhost:8080/statistics/not-a-real-id` → 404.

- [X] T009 [US3] Add cases to `StatisticLongTermRoutesKtTest.kt`: unknown `statisticId` → 404 + `ErrorDto`; known `statisticId` with zero rows in range → 200 + `[]` (not 404) (depends on T002)

**Checkpoint**: All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T010 Update `openapi.yaml` `From`/`To` param schemas to `oneOf: [integer int64, string date-time]` and add `400` response, per `contracts/statistics-endpoint.yaml` (depends on T003)
- [X] T011 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T010)
- [X] T012 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T011)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and T006 (extends the same test file).
- **User Story 3 (Phase 5)**: Depends on Phase 2; independent of Phase 4 (different test cases, same file — sequential, not parallel, to avoid edit conflicts).
- **Polish (Phase 6)**: Depends on all stories complete.

### Parallel Opportunities

- T001 has no dependency on anything else in Phase 2 and can start immediately; T002–T005 are sequential (each depends on the previous file existing).
- T007/T008/T009 all edit `StatisticLongTermRoutesKtTest.kt` — run sequentially, not in parallel, despite different story labels.

---

## Parallel Example: Phase 2

```bash
# T001 can start immediately; T002 waits on it.
Task: "Create StatisticDto in src/main/kotlin/com/anjo/monhomeass/model/dto/StatisticDto.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl` a known statisticId, confirm full history returns
4. Ship — US2/US3 are additive refinements, not blockers to a usable endpoint

### Incremental Delivery

1. Foundational → endpoint exists, unfiltered
2. + US1 test → confirmed correct for the base case (MVP)
3. + US2 → date-range filtering confirmed
4. + US3 → 404 behavior confirmed
5. + Polish → openapi.yaml sync, full build, quickstart validation
