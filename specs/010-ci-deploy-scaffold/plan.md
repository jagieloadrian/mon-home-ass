# Implementation Plan: CI Build Check & Deploy Scaffold

**Branch**: `010-ci-deploy-scaffold` | **Date**: 2026-07-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-ci-deploy-scaffold/spec.md`

## Summary

Close the gap in the existing `.github/workflows/build.yml` (add push-to-main
trigger alongside the existing pull_request trigger, still one job), add a
JaCoCo 80% coverage gate and a Codecov upload step to that same job, and
confirm the already-existing `.devops/{k8s,helm}` deploy scaffold still
satisfies User Story 2 as-is. Branch protection and the Codecov secret are
documented manual steps (no `gh` CLI/API token available in this
environment to apply them programmatically).

## Technical Context

**Language/Version**: Kotlin/Gradle 9.2.1 (existing) — no change

**Primary Dependencies**: GitHub Actions (`actions/checkout@v4`,
`actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`, new:
`codecov/codecov-action`), Gradle's built-in `jacoco` plugin (no new
external dependency — ships with Gradle core)

**Storage**: N/A

**Testing**: existing Gradle test suite; `jacocoTestCoverageVerification`
added to `check`, so `./gradlew build` is still the single verification
command

**Target Platform**: GitHub Actions (`ubuntu-latest` runner, confirmed by
the existing `.github/workflows/` path even though the git `origin` remote
is GitLab — `github` is a secondary remote and the only one with an
Actions workflow directory)

**Project Type**: single project — CI/deploy infrastructure change, no
application source code touched

**Performance Goals**: N/A

**Constraints**: FR-004a (branch protection) and the Codecov upload token
are GitHub repo *settings*/*secrets*, not files — cannot be applied by this
implementation without `gh` CLI or an API token (neither available here);
documented as manual post-merge steps instead (see `research.md`)

**Scale/Scope**: one workflow file edit (`build.yml`), one `build.gradle.kts`
edit (jacoco plugin + verification task), zero new files under `.devops/`
(already complete)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary** — PASS (N/A). No SQL, no recorder DB code
  touched by this feature.
- **II. YAGNI / Minimal Diff** — PASS. Matches roadmap Phase 10/11 scope
  exactly; the coverage/Codecov addition was explicitly requested by the
  user (not speculative), and is wired into the existing single job rather
  than a new stage, per FR-003's own constraint.
- **III. Token-Efficient Execution** — PASS (process constraint).
- **IV. Working App After Every Implementation** — PASS is the acceptance
  gate: `./gradlew build` (now including the coverage gate) must stay green.
  Risk: current coverage is unmeasured — see `research.md`; if it's under
  80%, the build going red is expected/correct per FR-010, not a bug to
  route around by lowering the number silently.
- **V. Test-Backed Changes** — PASS. No new application logic; this is
  build/CI/deploy tooling. If the coverage measurement task in `tasks.md`
  finds coverage under 80%, closing that gap means adding real tests for
  genuinely untested code — not a synthetic test to pad the number.

No violations — Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/010-ci-deploy-scaffold/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md
```

No `data-model.md`/`contracts/` — no entities, no new external interface;
this feature only changes CI workflow config and confirms existing deploy
manifests.

### Source Code (repository root)

**Structure Decision**: Single project (existing layout, unchanged):

```text
.github/workflows/build.yml   # add push:branches:[main] trigger, jacoco/codecov steps
build.gradle.kts              # add `jacoco` plugin + jacocoTestCoverageVerification (min 80%)
.devops/k8s/                  # existing — confirm via dry-run, no edits expected
.devops/helm/mon-home-ass/    # existing — confirm via helm lint/template, no edits expected
```

No new directories or application source files.

## Complexity Tracking

Not applicable — no Constitution Check violations.
