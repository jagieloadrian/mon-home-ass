# Research: CI Build Check & Deploy Scaffold

## Existing state (checked 2026-07-31)

- `.github/workflows/build.yml` already exists: one job, `ubuntu-latest`,
  `actions/checkout@v4` → `actions/setup-java@v4` (temurin 21) →
  `gradle/actions/setup-gradle@v4` → `./gradlew build`. Trigger is
  **`pull_request` only** — no `push: branches: [main]` (FR-001 gap).
- `.devops/k8s/{deployment,service,secret.example}.yaml` and
  `.devops/helm/mon-home-ass/{Chart.yaml,values.yaml,templates/*}` already
  exist and match the roadmap's Phase 11 description (Deployment/Service/
  Secret, config injected via `-config=/config/application.yaml`,
  placeholder secret, no Ingress/HPA/NetworkPolicy). **User Story 2 (FR-005–
  FR-009) appears already satisfied** — plan only needs to confirm this by
  rendering/validating, not build from scratch.
- Two git remotes configured: `origin` → GitLab
  (`git@gitlab.com:jagieloadrian/mon-home-ass.git`), `github` → GitHub
  (`git@github.com:jagieloadrian/mon-home-ass.git`). The existing
  `.github/workflows/*` path confirms GitHub Actions is the intended CI
  platform (GitLab CI would live in `.gitlab-ci.yml`, which doesn't exist).
- No `gh` CLI and no GitHub API token available in this environment.

## Branch protection (FR-004a)

- Decision: document as a **manual, one-time GitHub repository-settings
  step** (Settings → Branches → branch protection rule on `main` → require
  the `build` status check), not an automated task.
- Rationale: GitHub branch protection rules are repo *settings*, not a file
  in the repository — there's no `.github/branch-protection.yml` equivalent
  that GitHub reads automatically. Applying it programmatically needs the
  `gh` CLI or the GitHub API with a token with admin rights on the repo,
  neither of which is available in this environment.
- Alternatives considered: a Terraform/`github` provider config file — this
  would just be committed IaC that still needs someone with credentials to
  `terraform apply`; adds a new tool dependency for a one-time checkbox
  toggle. Rejected as over-engineering for what the roadmap frames as a
  scaffold-first, wire-it-later phase (consistent with Phase 11's own
  "not wired up yet" framing).
- Task-generation implication: this becomes a documented manual step in
  `quickstart.md`/README, with an explicit callout that it must be done by
  someone with admin access to the GitHub repo — not a task the
  implementation can mark verified by itself.

## Code coverage: JaCoCo (FR-010)

- Decision: use Gradle's built-in `jacoco` plugin (ships with Gradle core,
  no external dependency — ladder rung 5) with a
  `jacocoTestCoverageVerification` task enforcing `minimum = 0.80` on line
  coverage, wired into `check` (so `./gradlew build` already fails if
  coverage drops below 80%, no new CI step beyond what already runs).
- Rationale: matches Gradle-native approach already used for `test`/`check`
  wiring in this project; no new dependency needed.
- Risk flagged for implementation: **current coverage is unmeasured**.
  Adding an 80% gate could immediately fail the build if actual coverage is
  below that today. Per spec (FR-010's fixed 80% figure came directly from
  the user), the plan does NOT lower this number to whatever passes —
  implementation must measure actual coverage first and, if under 80%,
  surface that to the user before merging enforcement (add tests to close
  the gap, or get an explicit decision to phase in enforcement) rather than
  silently adjusting the threshold.

## Coverage reporting: Codecov (FR-011/FR-012)

- Decision: add `codecov/codecov-action` as a step in the same existing
  build job, uploading the JaCoCo XML report after the build step.
- Rationale: standard, first-party GitHub Action for Codecov, one step, no
  new job/stage (keeps FR-003's single-job constraint).
- Constraint: Codecov upload for a private-looking repo typically needs a
  `CODECOV_TOKEN` repository secret. This is another manual, credential-
  gated setup step (GitHub Settings → Secrets → Actions) that can't be
  applied by editing files alone — documented as a manual step alongside
  branch protection, not a task the agent can verify end-to-end here.
- Per FR-012 (upload failure must not fail the build): use
  `fail_ci_if_error: false` on the action (its documented default) so a
  Codecov outage doesn't block merges — only the JaCoCo threshold and
  existing test/build results gate the merge.

## User Story 2 validation approach

- Decision: validate the existing manifests with `kubectl apply
  --dry-run=client` (or `--server` if a cluster were available, which it
  isn't here) and `helm template`/`helm lint`, rather than writing new
  manifests.
- Rationale: per spec's Independent Test for US2 — these commands don't
  need a live cluster and directly verify the files already in
  `.devops/`.
