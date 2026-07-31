# Implementation Plan: Search filter on statistics-meta

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-statistics-meta-search/spec.md`

## Summary

`GET /statistics-meta?search=...` narrows the existing list to entries whose
`statistic_id` or `name` contains the given text, case-insensitively. Adds
an optional `search` parameter to the existing `StatisticMetaRepo.getAll()`
method (no new repo method, no new DTO) and reads the query param in the
existing `statisticMetaRoute`. SQL-side `LIKE` filter, not fetched-then-
filtered in Kotlin.

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: Ktor server + `ktor-server-routing-openapi`/swaggerUI, Exposed ORM, kotlinx.serialization — all already in `build.gradle.kts`

**Storage**: Postgres (readonly recorder DB) via existing `StatisticMetaData` Exposed table (`src/main/kotlin/com/anjo/monhomeass/model/tables/Statistic.kt`); H2 for tests

**Testing**: Kotlin test + Ktor test host, extending the existing `StatisticsMetaRoutesKtTest` (`src/test/kotlin/com/anjo/monhomeass/routing`) with `search` cases

**Target Platform**: Linux server (existing deployment)

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: N/A — matches existing endpoint; filter applied in the SQL `WHERE` clause via `LIKE`, not in application code (per FR-005)

**Constraints**: Readonly DB only (Principle I); single `search` param only, no `exclude`, no pagination — per spec Assumptions (matches roadmap Phase 6 wording exactly, Phase 7 pagination is a separate gated phase)

**Scale/Scope**: No new files — extends `StatisticMetaRepo.getAll()` with an optional param and `statisticMetaRoute` with a query param read; same shape/size as the smallest prior phases

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — single `SELECT ... WHERE ... LIKE`, no writes.
- **II. YAGNI / Minimal Diff**: PASS — reuses the existing `getAll()` method signature with an added optional parameter (default `null`, so the one existing caller is unaffected) rather than adding parallel `getAllWithSearch`/etc. methods; no `exclude` param, no pagination, matches spec's explicit YAGNI framing. Note: roadmap flags this whole phase as trigger-gated ("do NOT build until a client actually needs it") — proceeding on explicit user request, not because the trigger fired; flagged again here for visibility, not blocking.
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task.
- **V. Test-Backed Changes**: PASS (planned) — extends existing `StatisticsMetaRoutesKtTest` with `search` cases (match, no-match, empty/absent param) rather than a new test file, since no new route or repo class is introduced.

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/006-statistics-meta-search/
├── plan.md              # This file (/speckit-plan command output)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
src/main/kotlin/com/anjo/monhomeass/
├── model/tables/Statistic.kt           # existing — StatisticMetaData table (reused, no change)
├── repository/StatisticMetaRepo.kt     # EXTEND — getAll(search: String? = null)
└── routing/StatisticsRoutes.kt         # EXTEND — statisticMetaRoute reads `search` query param

src/test/kotlin/com/anjo/monhomeass/routing/
└── StatisticsMetaRoutesKtTest.kt       # EXTEND — search match/no-match/absent cases

openapi.yaml                            # EXTEND — add `search` query param to /statistics-meta
```

**Structure Decision**: Single existing Ktor project, no new module, no new
files. Smallest possible slice: one added optional parameter threaded
through the existing repo method and route, matching the spec's explicit
"no new fields, no new entity" framing.

## Complexity Tracking

No violations — table empty.
