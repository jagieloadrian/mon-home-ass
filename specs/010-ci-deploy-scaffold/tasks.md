---

description: "Task list for feature implementation"
---

# Tasks: CI Build Check & Deploy Scaffold

**Input**: Design documents from `/specs/010-ci-deploy-scaffold/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, quickstart.md

**Tests**: No new automated test tasks for the CI/deploy tooling itself — the build check (`./gradlew build`) is the verification mechanism. If the coverage baseline task (T004) finds real gaps, closing them means adding genuine unit tests, tracked as its own conditional task.

**Organization**: Tasks are grouped by user story to enable independent verification of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Include exact file paths in descriptions

## Path Conventions

Single project (existing layout): `.github/workflows/build.yml`,
`build.gradle.kts` at repo root; `.devops/k8s/`, `.devops/helm/mon-home-ass/`
already exist (User Story 2 confirms, doesn't create).

---

## Phase 1: Setup

**Purpose**: Confirm current state before changing anything.

- [X] T001 Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` on the unmodified branch and record BUILD SUCCESSFUL + test count as the pre-change baseline. Result: BUILD SUCCESSFUL, 61 tests.

---

## Phase 2: Foundational

No blocking infrastructure — both user stories touch disjoint files
(`build.gradle.kts`/`.github/workflows/build.yml` for US1,
`.devops/` for US2, which already exists). Proceed directly to Phase 3.

---

## Phase 3: User Story 1 - Automatic build verification on every change (Priority: P1) 🎯 MVP

**Goal**: Every push to main and every PR triggers one build check, that
check enforces an 80% coverage floor and publishes to Codecov, and merging
is blocked until it passes.

**Independent Test**: Push a commit to main and open a PR; both trigger the workflow; a PR cannot be merged while the check is red (once branch protection, a documented manual step, is applied).

### Implementation for User Story 1

- [X] T002 [US1] Add the `jacoco` plugin (Gradle built-in, no version catalog entry needed) to `build.gradle.kts`, with a `jacocoTestCoverageVerification` task requiring `minimum = 0.80` line coverage, wired so `check`/`build` depends on it. Done — `jacoco` plugin added, `jacocoTestReport` (XML enabled) runs after `test`, `jacocoTestCoverageVerification` (80% minimum) runs after the report, `check` depends on verification.
- [X] T003 [US1] Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` and record the actual overall coverage percentage from `build/reports/jacoco/test/html/index.html` (or the verification task's failure message if it's under 80%). Result: 63.0% initially — well under 80%.
- [X] T004 [US1] If T003 shows coverage under 80%: identify the least-covered, most-important source files (via the JaCoCo HTML report) and add real unit tests for genuinely untested behavior until coverage clears 80% — do not lower the 0.80 figure in `build.gradle.kts` to make the gate pass artificially (per spec FR-010 and constitution Principle V). If T003 already shows ≥80%, this task is a no-op — record that and move on. Found a real bug: `/swagger`'s OpenAPI-from-routes generation was silently broken (Ktor `async(LAZY)` + coroutine dispatch issue), leaving every route's `.describe{}` block dead code — fixed by building the doc eagerly at startup (`Routing.kt`), which alone raised coverage 63%→73%. Added ~20 real error-path tests (bad request/404/503/filter branches across statistics, migrations, schema-changes, events routes) raising it to 75%. Per user decision (asked mid-implementation via AskUserQuestion), stopped at 75% rather than keep grinding or delete production code — threshold in `build.gradle.kts` set to 0.74 (safe floor under measured 75%) instead of 0.80; spec.md FR-010/SC-005 updated to match via a new Clarification entry.
- [X] T005 [US1] Add a `push: branches: [main]` trigger alongside the existing `pull_request:` trigger in `.github/workflows/build.yml`'s single `on:` block (keep one job — do not duplicate the job per FR-003). Done.
- [X] T006 [US1] Add a `codecov/codecov-action` step to the same job in `.github/workflows/build.yml`, after the `./gradlew build` step, pointing at the JaCoCo XML report path (`build/reports/jacoco/test/jacocoTestReport.xml` or equivalent — confirm actual path from the plugin's default `jacocoTestReport` task output), with `fail_ci_if_error: false` so a Codecov outage doesn't fail the build (FR-012). Done — path confirmed as `build/reports/jacoco/test/jacocoTestReport.xml`.
- [X] T007 [US1] Validate the updated `.github/workflows/build.yml` is well-formed YAML (e.g. `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/build.yml'))"` if no dedicated YAML/Actions linter is installed). Result: valid.
- [X] T008 [US1] Re-run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build` and confirm `BUILD SUCCESSFUL` with the coverage gate passing (≥80%) and the same or greater test count than the Phase 1 baseline. Gate threshold is 0.74 per the Clarifications update (not 0.80) — result: BUILD SUCCESSFUL, coverage ~75%, 92 tests (up from 61 baseline).
- [X] T008a [US1] Confirm no automated deployment is triggered (FR-008/SC-004): grep `.github/workflows/build.yml` for any `kubectl`/`helm install`/`helm upgrade` step, and confirm `.github/workflows/` contains no other workflow file besides `build.yml` — the build job must only build/test/report coverage, never apply `.devops/` manifests. Confirmed — only `build.yml` exists, only steps are checkout/setup-java/setup-gradle/build/codecov-upload.

**Checkpoint**: Workflow triggers on both push-to-main and PR, single job, coverage gate passes, Codecov step present — User Story 1's automatable scope is done. (Branch protection itself is a manual GitHub-settings step — see Phase 5.)

---

## Phase 4: User Story 2 - Ready-to-apply deployment manifests (Priority: P2)

**Goal**: Confirm the already-existing `.devops/k8s` manifests and
`.devops/helm/mon-home-ass` chart still describe a valid
Deployment/Service/Secret shape with config injected via file path — per
`research.md`, this is a confirmation pass, not a from-scratch build.

**Independent Test**: Render/validate both the plain manifests and the Helm chart locally without a live cluster, per `quickstart.md` step 4.

### Implementation for User Story 2

- [X] T009 [P] [US2] Validate `.devops/k8s/deployment.yaml` and `.devops/k8s/service.yaml` are well-formed and describe a Deployment + Service for the app: use `kubectl apply --dry-run=client -f .devops/k8s/deployment.yaml -f .devops/k8s/service.yaml` if `kubectl` is installed; otherwise fall back to a plain YAML-parse validation (`python3 -c "import yaml,sys; [yaml.safe_load(d) for d in open(f).read().split('---')]"` per file) and a manual read confirming the config-file-path mount (FR-007). `kubectl` not installed in this environment — used the YAML-parse fallback: both files valid, one Deployment + one Service, `args: ["-config=/config/application.yaml"]` confirms the config-file-path mount matches FR-007.
- [X] T010 [P] [US2] Validate `.devops/helm/mon-home-ass` with `helm lint .devops/helm/mon-home-ass` and `helm template .devops/helm/mon-home-ass --set image.repository=example/mon-home-ass --set image.tag=0.0.1 --set db.url=localhost:5432 --set db.name=home_assistant --set db.user=home_ass_user --set db.password=placeholder`, confirming it renders the same Deployment/Service/Secret shape (FR-006). `helm lint`: 0 charts failed (1 minor info: missing icon, cosmetic). `helm template`: renders Secret + Service + Deployment, matching the plain manifests.
- [X] T011 [US2] Confirm `.devops/k8s/secret.example.yaml` contains no real credential value, only a placeholder shape (FR-009) — a manual read, not a scripted check (secrets are exactly the kind of file a naive grep-based "no secrets" check gives false confidence about). Confirmed: password is the literal placeholder `CHANGE_ME`, host is `postgres-host` — no real values.

**Checkpoint**: Deploy scaffold confirmed still valid as-is — User Story 2 done (no changes needed, per research.md).

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T012 Run `quickstart.md` end-to-end (all 4 automatable steps) as final sign-off. All 4 pass (see T005-T011 results above); quickstart.md's coverage figure updated 80%→74% to match the Clarifications decision.
- [X] T013 Confirm `quickstart.md` step 5 (manual, credential-gated steps: branch protection rule on `main` requiring the build check, and the `CODECOV_TOKEN` repo secret) is clearly documented as a required manual follow-up for whoever has GitHub admin access — this feature's automatable scope ends at the workflow file; these two steps cannot be verified by this implementation itself. Confirmed documented.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — establishes the baseline.
- **Foundational (Phase 2)**: Empty.
- **User Story 1 (Phase 3)**: Depends on Phase 1 baseline.
- **User Story 2 (Phase 4)**: Independent of Phase 3 — different files, already-existing manifests, can run in parallel with Phase 3 if desired.
- **Polish (Phase 5)**: Depends on both Phase 3 and Phase 4 being done.

### User Story Dependencies

- **User Story 1 (P1)**: No dependency on User Story 2.
- **User Story 2 (P2)**: No dependency on User Story 1 — genuinely independent, per spec.

### Within User Story 1

- T002 (add plugin) before T003 (measure coverage).
- T003 before T004 (conditional test-gap fix only makes sense once the number is known).
- T004 before T008 (final verify needs the gap closed, if any).
- T005/T006 (workflow edits) can happen in parallel with T002–T004 (different file) but T007 (YAML validation) and T008 (final build) come after both sets of edits land.
- T008a (deploy-absence check) can run any time after T005/T006 land — no dependency on T008's build result.

### Within User Story 2

- T009 and T010 are independent files/tools, both marked [P].
- T011 depends on nothing but is a quick manual read, can run anytime.

### Parallel Opportunities

- T002 (build.gradle.kts) and T005/T006 (build.yml) touch different files — parallelizable, though T007/T008 need both done first.
- T009 and T010 (US2) are marked [P] — different tools, different files.
- Phase 3 and Phase 4 can proceed in parallel as whole phases (no shared files).

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (baseline).
2. Complete Phase 3: User Story 1 (coverage gate + workflow triggers + Codecov step).
3. **STOP and VALIDATE**: run `quickstart.md` steps 1–3.
4. This alone delivers the roadmap's core Phase 10 ask; User Story 2 is confirmation of already-shipped work, not new value.

### Incremental Delivery

1. Phase 1 → Phase 3 (US1) and Phase 4 (US2) in parallel → Phase 5 (polish, document manual steps) → PR.

---

## Notes

- No [Story] tasks in Setup/Foundational/Polish — matches template convention.
- T004 is conditional — do not skip evaluating it, but it may resolve to "no-op" if coverage is already ≥80%.
- Branch protection and the Codecov secret (FR-004a, part of FR-011) are out of this implementation's automatable reach in this environment (no `gh` CLI/API token) — T013 documents them as required manual follow-ups rather than silently dropping them.
- Avoid: lowering the 80% threshold to make T003/T008 pass, adding a second CI job instead of extending the existing one, writing new `.devops/` manifests when the existing ones already satisfy User Story 2.
