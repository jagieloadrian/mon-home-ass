# Tasks: Statistic Entities List

**Input**: Design documents from `/specs/001-statistics-meta-list/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included — Constitution Principle V mandates a test for every new repo/route.

**Organization**: Single user story (US1, P1) — no P2/P3 exist for this feature.

## Format: `[ID] [P?] [Story] Description`

## Path Conventions

Single Kotlin/Ktor project — `src/main/kotlin/com/anjo/monhomeass/`, `src/test/kotlin/com/anjo/monhomeass/` (existing layout, no new modules).

---

## Phase 1: Setup

- [X] T001 Add `implementation("io.ktor:ktor-server-status-pages-jvm")` to `build.gradle.kts` (version resolved via the existing `io.ktor.plugin` BOM — no explicit version pin)

---

## Phase 2: Foundational (Blocking Prerequisite)

**Purpose**: FR-006's 503-on-DB-failure behavior is cross-cutting infra the US1 route depends on.

**⚠️ CRITICAL**: T002/T003 must complete before T008 (route wiring) can be verified end-to-end.

- [X] T002 [P] Implement `configureStatusPages()` in `src/main/kotlin/com/anjo/monhomeass/plugins/StatusPages.kt` — install Ktor `StatusPages`, catch `ExposedSQLException`/`SQLException`, respond `503 Service Unavailable` with a `{"message": "..."}` JSON body (see research.md decision)
- [X] T003 Call `configureStatusPages()` from the application module in `src/main/kotlin/com/anjo/monhomeass/Application.kt`

**Checkpoint**: Foundation ready — US1 implementation can proceed.

---

## Phase 3: User Story 1 - Discover trackable statistics (Priority: P1) 🎯 MVP

**Goal**: `GET /statistics-meta` returns every trackable HA statistic from the recorder DB, per data-model.md / contracts/statistics-meta.md.

**Independent Test**: Call `GET /statistics-meta` against a DB with known `statistics_meta` rows and confirm every row appears with correct fields; confirm empty DB → `[]`; confirm DB failure → `503`.

### Tests for User Story 1 ⚠️

> Write this test FIRST — it must FAIL (endpoint doesn't exist yet) before implementation.

- [X] T004 [P] [US1] Write `StatisticsMetaRoutesKtTest` in `src/test/kotlin/com/anjo/monhomeass/routing/StatisticsMetaRoutesKtTest.kt` covering: happy path (rows → `StatisticMetaDto` list, matching spec.md Acceptance Scenario 1), null optional fields passed through as null (Scenario 2), empty DB → `200` `[]` (FR-003), and DB/query failure → `503` with error body (FR-006) — follow `RecorderRoutesKtTest`'s `testApplication`/`TestDatabase` pattern

### Implementation for User Story 1

- [X] T005 [P] [US1] Create `StatisticMetaDto` (`@Serializable` data class per data-model.md) + `ResultRow.toStatisticMetaDto()` mapper in `src/main/kotlin/com/anjo/monhomeass/model/dto/StatisticMetaDto.kt`, reading `StatisticMetaData` table columns
- [X] T006 [US1] Create `StatisticMetaRepo.getAll(): List<StatisticMetaDto>` in `src/main/kotlin/com/anjo/monhomeass/repository/StatisticMetaRepo.kt` (`StatisticMetaData.selectAll().map { it.toStatisticMetaDto() }` inside `transaction { }`, mirroring `RecorderRepo`) — depends on T005
- [X] T007 [US1] Create `Route.statisticMetaRoute(repo: StatisticMetaRepo)` with `GET /statistics-meta` + `.describe { }` OpenAPI block in `src/main/kotlin/com/anjo/monhomeass/routing/StatisticsRoutes.kt`, matching `recorderRout`'s shape and the `200`/`503` responses in `openapi.yaml` — depends on T006
- [X] T008 [US1] Wire `StatisticMetaRepo` + `statisticMetaRoute(...)` into `configureRouting()` in `src/main/kotlin/com/anjo/monhomeass/plugins/Routing.kt` — depends on T007, T003
- [X] T009 [US1] Run `./gradlew test --tests "*StatisticsMetaRoutesKtTest*"`, confirm T004's test now passes — depends on T004, T008

**Checkpoint**: US1 fully functional and independently testable (this is the whole feature — no further stories).

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T010 Run `./gradlew build` (full suite) — must pass per Constitution IV before this implementation is considered done
- [X] T011 Walk through `quickstart.md` manually (curl happy path + DB-down 503 path) to confirm the automated tests match real behavior

---

## Dependencies & Execution Order

- **Setup (T001)**: no dependencies
- **Foundational (T002-T003)**: depends on T001 (dependency must be in the build first); T002/T003 block T008
- **US1 (T004-T009)**: T004 can start immediately (parallel with T002/T003); T005 parallel with T004; T006→T007→T008 sequential (same reason as existing repo→route pattern); T008 needs T003 done too; T009 needs both T004 and T008
- **Polish (T010-T011)**: after T009

### Parallel Example

```bash
# Needs T001 first (adds the dependency T002 installs):
Task: "Implement configureStatusPages() in plugins/StatusPages.kt"       # T002

# No prerequisite — can start immediately, in parallel with T001/T002:
Task: "Write StatisticsMetaRoutesKtTest in routing/StatisticsMetaRoutesKtTest.kt"  # T004
Task: "Create StatisticMetaDto in model/dto/StatisticMetaDto.kt"          # T005
```

---

## Implementation Strategy

Single story, no phased rollout needed: Setup → Foundational → US1 → Polish, in order. This *is* Phase 1 of `user-roadmap.md` in full — nothing held back for a later increment.

## Notes

- No P2/P3 — this feature is one endpoint, spec.md defines only US1.
- Contract source of truth is root `openapi.yaml` (see `contracts/statistics-meta.md`) — T007 must match it, not diverge.
- Commit after each task or logical group, per repo convention.
- SC-004 (<1s response, tens-hundreds of rows) is deliberately NOT covered by
  a dedicated perf task: a single unindexed `selectAll()` over a table this
  small meets it by construction, and a wall-clock assertion in CI would be
  flaky rather than useful. Revisit only if `statistics_meta` ever grows to
  a size where that assumption stops holding.
