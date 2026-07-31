---

description: "Task list for feature implementation"
---

# Tasks: Dependency Updates

**Input**: Design documents from `/specs/009-dependency-updates/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, quickstart.md

**Tests**: No new test tasks — the existing automated test suite (`./gradlew build`) is itself the acceptance gate (FR-002/SC-002); this feature adds no entities, endpoints, or non-trivial logic of its own.

**Organization**: Tasks are grouped by user story to enable independent verification of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Include exact file paths in descriptions

## Path Conventions

Single project (existing layout): `gradle.properties`, `build.gradle.kts` at
repo root; `src/main/kotlin/...`, `src/test/kotlin/...` for any corrective
source changes a bump surfaces.

---

## Phase 1: Setup

**Purpose**: Establish a known-good baseline before touching any version.

- [X] T001 Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` on the unmodified branch and record the result (build status, test count) as the pre-bump baseline to compare against later. Result: BUILD SUCCESSFUL, 61 tests.

---

## Phase 2: Foundational

No blocking infrastructure work — the version bump itself *is* User Story 1,
so there is nothing separate to set up first. Proceed directly to Phase 3.

---

## Phase 3: User Story 1 - Ship on current, supported dependency versions (Priority: P1) 🎯 MVP

**Goal**: All six `gradle.properties`-pinned dependencies (Kotlin, Ktor,
Exposed, PostgreSQL JDBC, H2, Logback) are on their current stable upstream
release, with any resulting breakage fixed, and the full test suite green.

**Independent Test**: Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` — `BUILD SUCCESSFUL`, same or greater test count than the Phase 1 baseline, zero skipped/removed tests.

### Implementation for User Story 1

- [X] T002 [US1] Re-check each dependency's current stable upstream release at implementation time (versions in `research.md` were current as of 2026-07-31 and may have moved) and update `gradle.properties`: `kotlin_version`, `ktor_version`, `postgres_version`, `h2_version`, `logback_version` (leave `exposed_version` unless a newer stable 1.x/2.x has since shipped). Same-day implementation — `research.md` versions still current: kotlin 2.4.0, ktor 3.5.1, postgres 42.7.13, h2 2.4.240, logback 1.6.1. Correction: exposed was NOT already latest — 1.3.1 shipped 2026-07-01 (missed in the initial research pass), bumped from 1.0.0 → 1.3.1, build/tests unaffected (61/61 still pass).
- [X] T003 [US1] Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` and capture the failure list (compile errors, test failures) if any. Result: BUILD SUCCESSFUL, 61/61 tests passing, zero failures — no breaking changes surfaced by this bump.
- [X] T004 [US1] Once Ktor is bumped past 3.4.1 (T002), verify whether `ktor-server-routing-openapi` is still required or now bundled into `ktor-server-swagger`/core, and remove the now-stale dependency line + comment in `build.gradle.kts` if so. Confirmed fixed by KTOR-9269 (3.4.1) — removed the explicit dependency + comment; `compileKotlin` still resolves `io.ktor.server.routing.openapi.*`/`io.ktor.openapi.*` imports across the routing package, and the full suite (61/61) still passes, so it's transitively pulled in now.
- [X] T005 [US1] Fix any Kotlin-2.4-surfaced compile errors (e.g. nullable Java-interop type-argument strictness, per `research.md`) in the specific source file(s) named by the compiler output under `src/main/kotlin/` or `src/test/kotlin/`. If the fix touches behavior that had no existing test coverage, add a test for it alongside the fix (per spec Edge Cases / constitution Principle V). N/A — T003 showed zero compile errors, no fix needed.
- [X] T006 [US1] Fix any H2-version-surfaced test failures (e.g. `datetime()`/`ubyte()` column type-mapping changes, per `research.md`) in the specific table/entity definition(s) named by the test failure output, most likely under `src/main/kotlin/com/anjo/monhomeass/` table objects. If the fix touches behavior that had no existing test coverage, add a test for it alongside the fix (per spec Edge Cases / constitution Principle V). N/A — T003 showed zero test failures, no fix needed.
- [X] T007 [US1] Fix any remaining Logback/PostgreSQL-JDBC-surfaced failures the same way — locate via the failing test's stack trace, fix the minimal source needed, no speculative changes beyond what the failure actually requires. If the fix touches behavior that had no existing test coverage, add a test for it alongside the fix (per spec Edge Cases / constitution Principle V). N/A — T003 showed zero failures, no fix needed.
- [X] T008 [US1] Re-run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` until it's `BUILD SUCCESSFUL` with the same test count as the Phase 1 baseline (zero tests removed/skipped to route around a break, per spec Edge Cases). Result: BUILD SUCCESSFUL, 61/61 tests — matches baseline exactly.
- [X] T009 [US1] Diff `build.gradle.kts` against the pre-bump baseline (T001) and confirm the only changes are version-property bumps and the T004 cleanup — no new dependency lines added (FR-006). Confirmed: `git diff build.gradle.kts` shows only the T004 removal (2 lines removed), zero additions.
- [X] T010 [US1] Manually start the app (`sh gradlew run`) and hit an existing endpoint plus `/swagger` per `quickstart.md` step 3, confirming no observable request/response shape or status-code change (FR-005/SC-004). Confirmed: app starts clean (DI resolves, "Application started"), `/swagger` → 200, `/statistics-meta` → 503 `{"message":"Recorder database unavailable"}` (expected in this sandbox — the real recorder Postgres at 192.168.0.23 isn't reachable here; same structured StatusPages error shape as before the bump, no new/different failure mode).

**Checkpoint**: All six dependencies on current stable, full suite green, no observable API behavior change — User Story 1 done.

---

## Phase 4: User Story 2 - Single reviewable change set (Priority: P2)

**Goal**: The bump lands as one PR, tests run once at the end covering the
final state — not one PR/commit per dependency.

**Independent Test**: `git log --oneline main..HEAD` and the resulting PR show one reviewable unit, per `quickstart.md` step 4.

### Implementation for User Story 2

- [X] T011 [US2] Stage all `gradle.properties`/`build.gradle.kts`/source changes from Phase 3 together and commit as a single logical change (may be one or a few commits, but merged as one PR — no per-dependency PRs or commits). Delegated to git-committer agent — single commit covering all Phase 3 changes plus the spec-kit artifacts.
- [ ] T012 [US2] Open a single PR for this change, with the test run from T008 as its verification evidence (not a fresh per-dependency run). Not opened — no explicit request to push/open a PR yet; commit is ready on the local branch.

**Checkpoint**: Change is reviewable as one unit — both user stories complete.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T013 [P] Update `specs/009-dependency-updates/research.md`'s version table if T002 found newer versions than those recorded on 2026-07-31, so the record matches what actually shipped. Updated — exposed 1.0.0 → 1.3.1 correction added to the version table and its own section (missed in initial research, caught post-implementation).
- [X] T014 Run `quickstart.md` end-to-end as a final sign-off before opening the PR from T012. Steps 1-3 confirmed via T002/T008/T010 above; step 4 (single-PR check) pending T012 (PR not yet opened, awaiting explicit user request to push).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — establishes the baseline.
- **Foundational (Phase 2)**: Empty — nothing blocks User Story 1.
- **User Story 1 (Phase 3)**: Depends on Phase 1 baseline existing.
- **User Story 2 (Phase 4)**: Depends on User Story 1 being green (T008) — can't assemble the single PR before the bump is actually working.
- **Polish (Phase 5)**: Depends on Phase 4 (T012 PR being about to open).

### User Story Dependencies

- **User Story 1 (P1)**: No dependency on other stories.
- **User Story 2 (P2)**: Depends on US1's completed, green state — this story is about *how* US1's result is delivered, not independent functionality.

### Within User Story 1

- T002 (bump) before T003 (build/observe failures).
- T003 before T004–T007 (fixes only make sense once failures are known).
- T004–T007 before T008 (re-verify after fixes).
- T008 before T009 (dependency-diff check) and T010 (manual behavior check — both only meaningful once the suite is green).

### Parallel Opportunities

- T004, T005, T006, T007 touch different files/concerns (build.gradle.kts vs. Kotlin-interop source vs. table definitions vs. other) and can be worked in parallel once T003 has identified which of them actually apply — but none are marked [P] above since which failures exist (if any) is only known after T003 runs, so treat them as conditional, not guaranteed-parallel.
- T013 is the only task genuinely independent of the PR-assembly sequence and is marked [P].

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (baseline).
2. Complete Phase 3: User Story 1 (the actual bump + fixes + green suite).
3. **STOP and VALIDATE**: run `quickstart.md` steps 1–3.
4. This alone is a legitimate, mergeable increment if US2's single-PR framing doesn't apply (e.g. solo contributor, no separate PR-hygiene concern).

### Incremental Delivery

1. Phase 1 → Phase 3 (US1) → validate → Phase 4 (US2, PR assembly) → Phase 5 (polish, open PR).

---

## Notes

- No [Story] tasks in Setup/Foundational — matches template convention.
- No new tests requested; the existing suite is the acceptance mechanism throughout.
- Commit after Phase 3 completes (green build) rather than per-file, matching US2's single-PR requirement.
- Avoid: bumping past what a re-check in T002 finds (no guessing beyond current stable), silently skipping a failing test, splitting this into multiple PRs.
