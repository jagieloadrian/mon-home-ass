# Implementation Plan: Long-term statistics for one entity

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-statistics-long-term/spec.md`

## Summary

`GET /statistics/{statisticId}` returns long-term statistic rows (mean,
min, max, sum, state, period start) for one entity, resolved by joining
`statistics` to `statistics_meta` on `metadata_id`, optionally bounded by
`from`/`to` query params (epoch seconds or ISO-8601). Follows the existing
table → DTO → repo → route pattern from Phase 1 (`statistics-meta`)
verbatim — no new abstraction layer.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via existing `Statistic` / `StatisticMetaData` Exposed tables (`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`); H2 for tests

**Testing**: Kotlin test + Ktor test host, following `StatisticsMetaRoutesKtTest` pattern (`src/test/kotlin/com/anjo/monhomeass/routing`)

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoints, no stated throughput target

**Constraints**: Readonly DB only (Principle I); no pagination (roadmap Phase 7 is trigger-gated, not yet triggered)

**Scale/Scope**: One new route, one new repo method, one DTO — same order of magnitude as Phase 1

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — only `selectAll`/`select` reads against `Statistic`/`StatisticMetaData`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — reuses existing tables, existing `epochSeconds` column type, existing route/DTO/repo pattern; no pagination/caching added.
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline, no extra artifacts beyond what's needed.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task, not a planning-time gate.
- **V. Test-Backed Changes**: PASS (planned) — new repo method and route ship with a route test per `StatisticsMetaRoutesKtTest` pattern.

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/002-statistics-long-term/
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
│   ├── tables/Statistic.kt          # existing — Statistic, StatisticMetaData tables (reused, no change)
│   └── dto/StatisticDto.kt          # NEW — DTO + ResultRow mapper
├── repository/
│   └── StatisticRepo.kt             # NEW — getLongTerm(statisticId, from?, to?)
├── routing/
│   └── StatisticsRoutes.kt          # EXTEND — add statisticRoute(repo) alongside existing statisticMetaRoute
└── plugins/
    ├── DependencyInjection.kt       # EXTEND — provide<StatisticRepo>
    └── Routing.kt                   # EXTEND — wire statisticRoute

src/test/kotlin/com/anjo/monhomeass/routing/
└── StatisticRoutesKtTest.kt         # NEW — happy path, from/to filtering, unknown id → 404

openapi.yaml                         # EXTEND — From/To params accept string|integer (epoch or ISO)
```

**Structure Decision**: Single existing Ktor project, no new module. Follows
the table → DTO → repo → route pattern already established by Phase 1
(`statistics-meta`); this phase only adds one route/repo/DTO to it.

## Complexity Tracking

No violations — table empty.
