# Implementation Plan: Statistic Entities List

**Branch**: `001-statistics-meta-list` | **Date**: 2026-07-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-statistics-meta-list/spec.md`

## Summary

Expose `GET /statistics-meta` returning every trackable HA statistic (id,
statistic_id, name, unit, source, has_mean, has_sum) from the readonly
recorder DB's `statistics_meta` table, following the existing table → DTO →
repo → route pattern. On DB/query failure, return 503 instead of an
unhandled 500 (FR-006) via a new, reusable `StatusPages` handler.

## Technical Context

**Language/Version**: Kotlin 2.3.0 / JVM 21

**Primary Dependencies**: Ktor 3.4.0 (server-core, routing-openapi, swaggerUI), Exposed 1.0.0 (jdbc/dao/kotlin-datetime), kotlinx.serialization

**Storage**: Postgres (readonly recorder DB) in prod, H2 in tests — both via Exposed, `Database.connect(... isReadOnly = true)`

**Testing**: `ktor-server-test-host` + `kotlin-test-junit` + `kotest-assertions-core`, following `RecorderRoutesKtTest`/`TestDatabase` pattern

**Target Platform**: JVM server, packaged as a Docker image via the `io.ktor.plugin` gradle plugin

**Project Type**: single-project web service (Ktor backend, no frontend)

**Performance Goals**: list returns in <1s for tens–hundreds of rows (SC-004) — no index/caching work needed at this volume

**Constraints**: readonly DB access only (Constitution I); no search/pagination/cache until a real need appears (Constitution II)

**Scale/Scope**: one new GET endpoint, one new repo, one new DTO; reuses the already-modeled `StatisticMetaData` table — no schema/migration work

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. Readonly Data Boundary | Feature only `selectAll()`s `statistics_meta`, no write/DDL | PASS |
| II. YAGNI / Minimal Diff | Reuses existing table→DTO→repo→route pattern; no search/pagination/cache added (those stay Phase 6-8, trigger-gated) | PASS |
| III. Token-Efficient Execution | This plan itself kept to what's needed; no speculative sections | PASS |
| IV. Working App After Implementation | Task list ends with `./gradlew build` passing | PASS (enforced at implement time) |
| V. Test-Backed Changes | New repo/route ship with a route test (`StatisticsMetaRoutesKtTest`) covering happy path + 503 path | PASS |

No violations — Complexity Tracking section not needed.

**Post-Phase-1 re-check**: Design (data-model.md, contracts/) introduces one
new plugin (`StatusPages`, Phase 1 below) beyond the base table→DTO→repo→route
pattern. This is required by FR-006 (spec-mandated, not speculative) and is a
stock Ktor plugin, not a custom abstraction — still PASS under Principle II.

## Project Structure

### Documentation (this feature)

```text
specs/001-statistics-meta-list/
├── plan.md              # this file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/            # Phase 1 output
└── tasks.md              # Phase 2 output (/speckit-tasks, not this command)
```

### Source Code (repository root)

Existing single-module Kotlin/Ktor project — no new modules/projects.

```text
src/main/kotlin/com/anjo/monhomeass/
├── model/
│   ├── tables/Statistic.kt          # EXISTING — StatisticMetaData table, reused as-is
│   └── dto/StatisticMetaDto.kt      # NEW — response DTO + ResultRow mapper
├── repository/StatisticMetaRepo.kt  # NEW — getAll(): List<StatisticMetaDto>
├── routing/StatisticsRoutes.kt      # NEW — GET /statistics-meta, describe{} for OpenAPI
└── plugins/
    ├── Routing.kt                   # EDIT — wire StatisticMetaRepo + route
    └── StatusPages.kt               # NEW — maps DB/query failures to 503 (FR-006)

src/test/kotlin/com/anjo/monhomeass/routing/
└── StatisticsMetaRoutesKtTest.kt    # NEW — happy path + empty list + 503-on-DB-failure
```

**Structure Decision**: Single existing Ktor project; feature adds files
inside the established `model/dto`, `repository`, `routing`, `plugins`
packages — no new package or module needed.

## Complexity Tracking

*No violations — table not needed.*
