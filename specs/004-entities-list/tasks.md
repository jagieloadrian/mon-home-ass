---

description: "Task list template for feature implementation"
---

# Tasks: Entities list

**Input**: Design documents from `/specs/004-entities-list/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/entities-endpoint.yaml, quickstart.md

**Tests**: Included — Constitution Principle V requires new repos/routes ship with a test.

**Organization**: Single user story (spec.md has only P1) — the entire feature is
one story, so Foundational and User Story 1 collapse into one build-then-test
sequence, following the exact Phase 1 (`statistics-meta`) file pattern.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

No new setup needed — existing Ktor project, `StateMetadata` table already
modeled, DI/Routing/StatusPages plugins already wired. Nothing to do here.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DTO + repo + route — the entire feature, since there is only one user story.

- [X] T001 [P] Create `EntityDto` (`id: Int`, `entityId: String?`) and `ResultRow.toEntityDto()` mapper in `src/main/kotlin/com/anjo/monhomeass/model/dto/EntityDto.kt`
- [X] T002 Create `StateMetadataRepo` in `src/main/kotlin/com/anjo/monhomeass/repository/StateMetadataRepo.kt` with `getAll(): List<EntityDto>` — `StateMetadata.selectAll().map { it.toEntityDto() }`, identical shape to `StatisticMetaRepo.getAll()` (depends on T001)
- [X] T003 Add `entitiesRoute(repo: StateMetadataRepo)` to `src/main/kotlin/com/anjo/monhomeass/routing/EntitiesRoutes.kt`: `GET /entities`, `.describe { }` block per existing `statisticMetaRoute` pattern (200 + 503 responses) (depends on T002)
- [X] T004 Provide `StateMetadataRepo` in `src/main/kotlin/com/anjo/monhomeass/plugins/DependencyInjection.kt` (depends on T002)
- [X] T005 Wire `entitiesRoute(stateMetadataRepo)` into `configureRouting()` in `src/main/kotlin/com/anjo/monhomeass/plugins/Routing.kt` (depends on T003, T004)

**Checkpoint**: `GET /entities` compiles and serves.

---

## Phase 3: User Story 1 - Discover known entities (Priority: P1) 🎯 MVP

**Goal**: `GET /entities` returns all known entities.

**Independent Test**: `curl localhost:8080/entities` → 200, array of `{id, entityId}`.

- [X] T006 [US1] Add `EntitiesRoutesKtTest.kt` in `src/test/kotlin/com/anjo/monhomeass/routing/`: happy path (seeded rows → 200 + expected array, including a null `entityId` row), empty list (no rows → 200 `[]`), and DB-down (table not created → 503 + `ErrorDto`) — same three-case shape as `StatisticsMetaRoutesKtTest`

**Checkpoint**: Feature fully functional and testable — this is the whole feature.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T007 Fix `openapi.yaml` `EntityDto.entityId` to `nullable: true` (matches `states_meta.entity_id` column; currently marked required in the existing stub) and add the `503` response to `/entities`, per `contracts/entities-endpoint.yaml` (depends on T003)
- [X] T008 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV) (depends on T001–T007)
- [X] T009 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior (depends on T008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. Delivers the whole feature (single user story).
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **Polish (Phase 4)**: Depends on Phase 3 complete.

### Parallel Opportunities

- T001 has no dependency and can start immediately; T002–T005 are sequential (each depends on the previous file existing).
- This phase touches only new files plus additive edits to shared plugin files (`DependencyInjection.kt`, `Routing.kt`) — no overlap with Phase 2/3's statistics files, safe to work independently of any other in-flight phase.

---

## Parallel Example: Phase 2

```bash
# T001 can start immediately; T002 waits on it.
Task: "Create EntityDto in src/main/kotlin/com/anjo/monhomeass/model/dto/EntityDto.kt"
```

---

## Implementation Strategy

### MVP First (and only)

1. Complete Phase 2: Foundational
2. Complete Phase 3: User Story 1
3. **STOP and VALIDATE**: `curl localhost:8080/entities`, confirm array returns
4. Complete Phase 4: Polish (openapi sync, build, quickstart)
5. Ship — this is the entire feature, no incremental stories to add later
