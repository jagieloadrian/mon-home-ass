# Implementation Plan: Dependency Updates

**Branch**: `009-dependency-updates` | **Date**: 2026-07-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-dependency-updates/spec.md`

## Summary

Bump the six version-pinned core dependencies in `gradle.properties`
(Kotlin, Ktor, Exposed, PostgreSQL JDBC, H2, Logback) to their current
stable upstream releases — including crossing a major version boundary if
one becomes available, per this feature's Clarification — fix any breaking
changes the bump surfaces, and land it as a single PR verified by one full
run of the existing test suite. Per `research.md`, no major-version
boundary is actually available for any of these six today (all are
minor/patch bumps), so this pass is a straightforward version-property
change plus whatever mechanical fixes the full build/test run surfaces.

## Technical Context

**Note**: all versions below were current stable as of 2026-07-31
(`research.md`); re-verify against current upstream at implementation time
(FR-001/FR-007) rather than treating these as fixed targets.

**Language/Version**: Kotlin 2.3.0 → 2.4.0 (JVM target, K2 compiler)

**Primary Dependencies**: Ktor 3.4.0 → 3.5.1 (server framework), Exposed
1.0.0 (ORM, no change), PostgreSQL JDBC 42.7.2 → 42.7.13, H2 2.2.224 →
2.4.240 (test DB), Logback 1.5.13 → 1.6.1 (SLF4J backend)

**Storage**: PostgreSQL (readonly Home Assistant recorder DB, production);
H2 (in-memory, tests only) — neither's data model changes, only driver/
engine versions

**Testing**: existing Gradle test suite — `kotlin-test-junit`, Kotest
assertions, MockK, Testcontainers (Postgres) — run via `./gradlew build`

**Target Platform**: JVM server (Ktor Netty engine), Linux, same as today

**Project Type**: single project (web service) — no structural change

**Performance Goals**: N/A — maintenance upgrade, no new performance target
(SC-004: zero observable behavior change is the bar, not a speed target)

**Constraints**: FR-005 — no observable API behavior change; FR-004 —
single PR; FR-006 — no unrelated new runtime dependencies

**Scale/Scope**: 6 version properties in `gradle.properties`; no new
files, tables, DTOs, repos, or routes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary** — PASS. No SQL statements change; only the
  JDBC driver and ORM library versions move. Nothing in this feature issues
  write/update/delete against the recorder DB.
- **II. YAGNI / Minimal Diff** — PASS. This is exactly the roadmap's
  Phase 9 trigger-gated item, scoped to the same 6 dependencies the roadmap
  names; no speculative abstraction introduced. FR-006 explicitly bars
  scope creep into unrelated new dependencies.
- **III. Token-Efficient Execution** — PASS (process constraint on
  execution, not a design gate).
- **IV. Working App After Every Implementation** — PASS is the acceptance
  gate itself: FR-002/SC-002 require `./gradlew build` (compile + full test
  suite) to pass before this feature is considered done.
- **V. Test-Backed Changes** — PASS. No new repos/routes/non-trivial logic
  is being added, so no new dedicated test is mandated by this principle;
  the existing suite re-run is what proves the bump safe. Per the spec's
  Edge Cases, if a bump breaks previously-untested behavior, the fix for
  that must come with a test (same as principle V already requires for any
  non-trivial logic change).

No violations — Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/009-dependency-updates/
├── plan.md              # This file
├── research.md          # Phase 0 output — version findings + breaking-change checks
├── quickstart.md         # Phase 1 output — validation steps
└── checklists/
    └── requirements.md  # Spec quality checklist (from /speckit-specify)
```

No `data-model.md` or `contracts/` — this feature introduces no entities
and no new/changed external interface (FR-005 explicitly requires the
opposite: zero observable API change), so both are skipped as not
applicable.

### Source Code (repository root)

**Structure Decision**: Single project (existing layout, unchanged):

```text
gradle.properties        # the 6 version properties being bumped
build.gradle.kts         # dependency declarations that read those properties
                          # (also re-check the routing-openapi comment/coupling
                          # noted in research.md once on Ktor 3.5.1)
src/main/kotlin/...       # touched only if a bump breaks compilation
src/test/kotlin/...       # touched only if a bump breaks a test; existing
                          # suite is the verification mechanism (FR-002)
```

No new directories. No new source files are anticipated; any touched under
`src/` are corrective (fixing a break), not additive.

## Complexity Tracking

Not applicable — no Constitution Check violations.
