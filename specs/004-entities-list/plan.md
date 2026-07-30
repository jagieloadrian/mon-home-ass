# Implementation Plan: Entities list

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-entities-list/spec.md`

## Summary

`GET /entities` returns the list of known Home Assistant entities from
`states_meta` (`metadata_id`, `entity_id`). Follows the Phase 1
(`statistics-meta`) pattern verbatim: table object (already modeled as
`StateMetadata`) → new `EntityDto` → new `StateMetadataRepo` → new
`entitiesRoute`. No filtering, pagination, or joins — simplest possible
slice.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via existing `StateMetadata` Exposed table (`src/main/kotlin/com/anjo/monhomeass/model/tables/State.kt`); H2 for tests

**Testing**: Kotlin test + Ktor test host, following `StatisticsMetaRoutesKtTest` pattern (`src/test/kotlin/com/anjo/monhomeass/routing`) — includes the 503-on-query-failure case since that's this API's established behavior for list endpoints

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoints, no stated throughput target

**Constraints**: Readonly DB only (Principle I); no pagination/search — roadmap doesn't gate this phase behind either, and the table is small (one row per entity)

**Scale/Scope**: One new DTO, one new repo, one new route — same shape as Phase 1, no join needed (`states_meta` is queried standalone, unlike statistics tables)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — only `selectAll()` against `StateMetadata`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — reuses the exact Phase 1 table→DTO→repo→route pattern, no new abstraction, no filtering/pagination added.
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task.
- **V. Test-Backed Changes**: PASS (planned) — new repo and route ship with a route test per `StatisticsMetaRoutesKtTest` pattern (happy path, empty list, 503).

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
src/main/kotlin/com/anjo/monhomeass/
├── model/
│   ├── tables/State.kt              # existing — StateMetadata table (reused, no change)
│   └── dto/EntityDto.kt             # NEW — DTO + ResultRow mapper
├── repository/
│   └── StateMetadataRepo.kt         # NEW — getAll()
├── routing/
│   └── EntitiesRoutes.kt            # NEW — entitiesRoute(repo)
└── plugins/
    ├── DependencyInjection.kt       # EXTEND — provide<StateMetadataRepo>
    └── Routing.kt                   # EXTEND — wire entitiesRoute

src/test/kotlin/com/anjo/monhomeass/routing/
└── EntitiesRoutesKtTest.kt          # NEW — happy path, empty list, 503

openapi.yaml                         # EXTEND — EntityDto.entityId: nullable (matches states_meta.entity_id column, currently marked required in the stub)
```

**Structure Decision**: Single existing Ktor project, no new module. Follows
the table → DTO → repo → route pattern already established by Phase 1
(`statistics-meta`) exactly — simplest of all phases so far, no join.

## Complexity Tracking

No violations — table empty.
