# Implementation Plan: Short-term (5-minute) statistics for one entity

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-statistics-short-term/spec.md`

## Summary

`GET /statistics/{statisticId}/short-term` returns short-term (5-minute
interval) statistic rows for one entity — identical row shape to Phase 2's
`StatisticDto`, same `from`/`to` contract, same 404/400 behavior. Backed by
the `statistics_short_term` table (`StatisticShortTem` Exposed object),
joined to `statistics_meta` exactly like Phase 2. Reuses `StatisticDto`
verbatim (roadmap's explicit note: "one DTO covers both") — only a new repo
method and route are added.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via `StatisticShortTem` / `StatisticMetaData` Exposed tables (`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`); H2 for tests

**Testing**: Kotlin test + Ktor test host, following `StatisticLongTermRoutesKtTest` pattern (`src/test/kotlin/com/anjo/monhomeass/routing`)

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoints, no stated throughput target

**Constraints**: Readonly DB only (Principle I); no pagination (roadmap Phase 7 is trigger-gated, covers both long- and short-term, not yet triggered); reuse `StatisticDto` — no new DTO for this phase (roadmap explicit YAGNI note)

**Scale/Scope**: One new repo method, one new route — same order of magnitude as Phase 2, smaller (no new DTO)

## Pre-existing bugs blocking this phase (found during Phase 2, applies here too)

`StatisticShortTem` (`Statistic.kt:39`) has two defects carried over from
before Phase 2 existed, same class as the bug fixed in Phase 2 for
`Statistic`:

1. `metadataId` references `StateMetadata.id` (the `states_meta` table)
   instead of `StatisticMetaData.id` (`statistics_meta`) — wrong FK target,
   would break the join this phase needs, same as Phase 2's bug.
2. `Table()` has no explicit table name — defaults to a derived name from
   the class name (`StatisticShortTem`, note the missing "r"), not the real
   `statistics_short_term` table. Never caught before because nothing
   queried this table until now.

Both must be fixed as part of this phase's implementation (not a separate
follow-up) — same reasoning as the Phase 2 fix: the bug is in code this
phase directly depends on.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — only reads against `StatisticShortTem`/`StatisticMetaData`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — reuses `StatisticDto`, `StatisticMetaData` lookup, and the exact route/repo pattern from Phase 2; no new abstraction.
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task.
- **V. Test-Backed Changes**: PASS (planned) — new repo method and route ship with a route test per `StatisticLongTermRoutesKtTest` pattern.

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/003-statistics-short-term/
├── plan.md              # This file
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
src/main/kotlin/com/anjo/monhomeass/
├── model/
│   ├── tables/Statistic.kt          # FIX — StatisticShortTem: correct FK target + explicit table name
│   └── dto/StatisticDto.kt          # existing — reused verbatim, no change
├── repository/
│   └── StatisticRepo.kt             # EXTEND — add getShortTerm(statisticId, from?, to?)
├── routing/
│   └── StatisticsRoutes.kt          # EXTEND — add statisticShortTermRoute(repo) alongside existing routes
└── plugins/
    └── Routing.kt                   # EXTEND — wire statisticShortTermRoute (reuses existing StatisticRepo DI binding)

src/test/kotlin/com/anjo/monhomeass/routing/
└── StatisticShortTermRoutesKtTest.kt  # NEW — happy path, from/to filtering, unknown id → 404

openapi.yaml                         # EXTEND — /statistics/{statisticId}/short-term: add 400 response (200/404 already present)
```

**Structure Decision**: Single existing Ktor project, no new module. Extends
`StatisticRepo`/`StatisticsRoutes` from Phase 2 with a short-term variant;
no new DTO, no new DI entry (same `StatisticRepo` bean already provided).

## Complexity Tracking

No violations — table empty.
