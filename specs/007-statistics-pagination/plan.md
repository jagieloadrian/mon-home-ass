# Implementation Plan: Pagination on long/short-term statistics

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-statistics-pagination/spec.md`

## Summary

`GET /statistics/{statisticId}` and `.../short-term` accept optional
`limit`/`offset` query params, applied via SQL `LIMIT`/`OFFSET` after the
existing `from`/`to` range filter and oldest-first ordering. No new files —
extends `StatisticRepo.getLongTerm`/`getShortTerm` and the two existing
route handlers in `StatisticsRoutes.kt`.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via existing `Statistic`/`StatisticShortTem`/`StatisticMetaData` Exposed tables; H2 for tests

**Testing**: Kotlin test + Ktor test host, extending the existing `StatisticLongTermRoutesKtTest`/`StatisticShortTermRoutesKtTest` with `limit`/`offset` cases

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoints; slicing done via Postgres `LIMIT`/`OFFSET` (per FR-007), not fetched-then-sliced in Kotlin

**Constraints**: Readonly DB only (Principle I); no max cap on `limit`, no response envelope — per spec Assumptions (YAGNI, matches roadmap Phase 7 wording exactly)

**Scale/Scope**: No new files — extends `StatisticRepo.getLongTerm`/`getShortTerm` with two optional params each and the two existing route handlers with query-param reads; same shape/size as Phase 6

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — `SELECT ... LIMIT ... OFFSET`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — extends existing method signatures with two optional params (default `null`, so both existing callers are unaffected) rather than new methods/DTOs; no envelope, no max cap, matches spec's explicit YAGNI framing. Note: roadmap flags this whole phase as trigger-gated ("do NOT build until a real result set is too big") — proceeding on explicit user request, not because the trigger fired; flagged again here for visibility, not blocking (same situation as Phase 6/`006-statistics-meta-search`).
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task.
- **V. Test-Backed Changes**: PASS (planned) — extends existing `StatisticLongTermRoutesKtTest`/`StatisticShortTermRoutesKtTest` with pagination cases rather than new test files, since no new route or repo class is introduced.

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/007-statistics-pagination/
├── plan.md              # This file (/speckit-plan command output)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
src/main/kotlin/com/anjo/monhomeass/
├── model/tables/Statistic.kt           # existing — Statistic/StatisticShortTem tables (reused, no change)
├── repository/StatisticRepo.kt         # EXTEND — getLongTerm/getShortTerm gain limit: Int?/offset: Long? params
└── routing/StatisticsRoutes.kt         # EXTEND — statisticRoute/statisticShortTermRoute read limit/offset query params, 400 on invalid values

src/test/kotlin/com/anjo/monhomeass/routing/
├── StatisticLongTermRoutesKtTest.kt    # EXTEND — limit/offset cases
└── StatisticShortTermRoutesKtTest.kt   # EXTEND — limit/offset cases

openapi.yaml                            # EXTEND — add limit/offset query params to both endpoints
```

**Structure Decision**: Single existing Ktor project, no new module, no new
files. Same shape as Phase 6 (`006-statistics-meta-search`): two added
optional parameters threaded through existing repo methods and routes,
applied identically to both the long-term and short-term variant per
FR-008.

## Complexity Tracking

No violations — table empty.
