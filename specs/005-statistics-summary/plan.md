# Implementation Plan: Statistics date-range summary

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-statistics-summary/spec.md`

## Summary

`GET /statistics/{statisticId}/summary` returns one aggregate row
(MIN/MAX/AVG/SUM of `state`) over the `statistics` table for a given
`statisticId`, optionally bounded by `from`/`to`. Resolves `statisticId` →
`metadata_id` via `statistics_meta` (same lookup as the existing
`/statistics/{statisticId}` endpoint, reusing its 400/404/503 semantics).
Aggregation happens in SQL, not in Kotlin — new `StatisticSummaryDto`, one
new repo method on the existing `StatisticRepo`, one new route function.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via existing `Statistic`/`StatisticMetaData` Exposed tables (`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`); H2 for tests

**Testing**: Kotlin test + Ktor test host, following `StatisticsRoutesKtTest`/long-term route test pattern (`src/test/kotlin/com/anjo/monhomeass/routing`) — covers the 200/200-empty/404/400/503 cases

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoints; aggregation done by Postgres SQL functions (`MIN`/`MAX`/`AVG`/`SUM`), not fetched-then-reduced in Kotlin, so cost stays flat regardless of range size

**Constraints**: Readonly DB only (Principle I); long-term `statistics` table only, no short-term variant, no grouping/bucketing params beyond `from`/`to` (per spec Assumptions / roadmap Phase 5 scope)

**Scale/Scope**: One new DTO, one new repo method (on existing `StatisticRepo`), one new route function — same shape as Phase 2's long-term lookup, aggregate instead of row-list

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — single `SELECT` with aggregate functions against `Statistic`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — reuses existing `StatisticRepo`/`statisticMetaId` lookup pattern, no new abstraction, no grouping/pagination added.
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task.
- **V. Test-Backed Changes**: PASS (planned) — new repo method and route ship with a route test per existing long-term route test pattern (200 with data, 200 empty-range, 404 unknown id, 400 bad range, 503 db down).

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/005-statistics-summary/
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
│   ├── tables/Statistic.kt          # existing — Statistic/StatisticMetaData tables (reused, no change)
│   └── dto/StatisticSummaryDto.kt   # NEW — aggregate DTO + ResultRow mapper
├── repository/
│   └── StatisticRepo.kt             # EXTEND — getSummary(statisticId, from, to): StatisticSummaryDto?
├── routing/
│   └── StatisticsRoutes.kt          # EXTEND — statisticSummaryRoute(repo), GET /statistics/{statisticId}/summary
└── plugins/
    └── Routing.kt                   # EXTEND — wire statisticSummaryRoute

src/test/kotlin/com/anjo/monhomeass/routing/
└── StatisticsRoutesKtTest.kt        # EXTEND — summary happy path, empty-range, 404, 400, 503

openapi.yaml                         # EXTEND — StatisticSummaryDto schema + /statistics/{statisticId}/summary path
```

**Structure Decision**: Single existing Ktor project, no new module. Reuses
`Statistic`/`StatisticMetaData` tables and the existing `StatisticRepo`
class (adds a method rather than a new repo, since it's the same table and
same `statisticId`→`metadataId` resolution already implemented for
`getLongTerm`). No DI/plugin change needed beyond wiring the new route
function — `StatisticRepo` is already provided.

## Complexity Tracking

No violations — table empty.
