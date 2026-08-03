# Feature Specification: CI Build Check & Deploy Scaffold

**Feature Branch**: `010-ci-deploy-scaffold`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 10 & 11"

## Clarifications

### Session 2026-07-31

- Q: Should the build check just be visible, or should it also be configured as a required status check that blocks merging? → A: Enforced — this feature also includes configuring the repo's branch protection rule so the build check is required before a PR can merge.
- Q: Should the build check also measure and enforce code coverage? → A: Yes — add a code coverage tool with an 80% minimum threshold enforced as part of the same build check, plus integration with an external coverage-reporting service in CI.
- Q: Actual measured coverage at implementation time was 63%, and closing the full gap to 80% required either open-ended test-writing with diminishing returns or deleting production code (unused table definitions) — how to proceed? → A: Ship at the coverage level actually reached (75%, after a real bug fix plus genuine error-path tests) rather than keep grinding or delete production code; the enforced threshold is set to 74% (a safe floor just under the measured 75%) instead of 80%.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic build verification on every change (Priority: P1)

As a maintainer, I want every push to the main branch and every pull request
to automatically trigger a full build, so a broken change is caught before
it lands on main or gets merged, without anyone having to remember to run
the build by hand.

**Why this priority**: This is the safety net that catches regressions the
earliest and cheapest — before merge, not after. It has no dependency on
anything else in this feature and delivers value the moment it's wired up.

**Independent Test**: Can be fully tested by pushing a commit directly to
main and separately by opening a pull request, and confirming both trigger
the automated build check and report pass/fail.

**Acceptance Scenarios**:

1. **Given** a commit is pushed directly to the main branch, **When** the
   push completes, **Then** an automated build check runs and reports its
   result.
2. **Given** a pull request is opened or updated, **When** the changes are
   pushed, **Then** an automated build check runs against those changes and
   reports its result.
3. **Given** the automated build check fails, **When** a maintainer views
   the pull request or commit, **Then** the failure is clearly visible
   before the change is merged.
4. **Given** the automated build check has not completed or has failed,
   **When** someone attempts to merge the pull request, **Then** the
   merge is blocked until the check passes.
5. **Given** a change drops overall code coverage below the required
   minimum, **When** the build check runs, **Then** the check fails and the
   merge is blocked the same as any other build failure.
6. **Given** the build check completes (pass or fail), **When** it
   finishes, **Then** the resulting coverage report is published to an
   external coverage-reporting service so coverage trends are visible
   outside the CI log.

---

### User Story 2 - Ready-to-apply deployment manifests (Priority: P2)

As the person responsible for eventually running this app somewhere other
than a developer machine, I want a ready-made set of deployment manifests
(plain Kubernetes and a Helm chart) describing how the app should run in a
cluster, so that when a real cluster and registry exist, deployment is a
matter of filling in environment-specific values rather than designing the
deployment shape from scratch.

**Why this priority**: Lower priority than automatic build verification
because there is no cluster to deploy to yet — this is preparatory scaffold,
not something exercised on every change. It's still valuable to have ready
rather than designed under time pressure once a cluster does exist.

**Independent Test**: Can be fully tested by rendering/validating the plain
manifests and the Helm chart locally (e.g. `kubectl apply --dry-run` /
`helm template`) without needing a live cluster, and confirming they
describe a Deployment, Service, and Secret for the app with configuration
injected via a config file path.

**Acceptance Scenarios**:

1. **Given** the plain Kubernetes manifests, **When** they are applied to a
   cluster with a suitable secret and image available, **Then** the app
   starts up reading its configuration from the injected config file path.
2. **Given** the Helm chart, **When** it is installed with the required
   values supplied (image, database connection details), **Then** it
   renders the same Deployment/Service/Secret shape as the plain manifests.
3. **Given** no cluster or image registry exists yet, **When** this feature
   is delivered, **Then** nothing in the existing build or deploy process
   changes automatically — applying the manifests remains a manual,
   deliberate action.

---

### Edge Cases

- What happens when the automated build check itself fails to run (e.g.
  infrastructure issue) rather than the build failing legitimately? This is
  treated the same as a failing build from the maintainer's point of view —
  it MUST NOT be silently treated as a pass; the change is not considered
  verified until a check actually completes and passes.
- How does the system handle a pull request with no code changes (e.g.
  docs-only)? The same single build check still runs — no separate
  lightweight path is introduced, since a single job already covers both
  triggers cheaply.
- What happens when code coverage falls below the required minimum? The
  build check fails, just as it would for a compile error or a failing
  test — no separate "coverage-only" warning state that lets the PR merge
  anyway.
- What happens if the external coverage-reporting service is unreachable or
  the upload step fails? The coverage upload failing MUST NOT be treated as
  a build failure by itself (it's a reporting side-effect, not part of
  correctness) — but the coverage-threshold check inside the build itself
  still gates the merge regardless of whether the upload succeeded.
- How does the system handle the deployment manifests being applied without
  the required secret/config first existing? The app fails to start in a
  visible way (missing config), the same as running it locally without
  `application.yaml` — no special-cased error handling is added for this
  manual, deliberate step.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST automatically run a full build check when a
  commit is pushed to the main branch.
- **FR-002**: The system MUST automatically run a full build check when a
  pull request is opened or updated.
- **FR-003**: Both triggers (push to main, pull request) MUST be covered by
  a single build check definition, not separate parallel checks that could
  drift out of sync with each other.
- **FR-004**: The build check's pass/fail result MUST be visible on the
  commit/pull request it ran against.
- **FR-004a**: The build check MUST be configured as a required status
  check on the main branch, so a pull request cannot be merged while the
  check is failing or has not yet completed. This is a repository-settings
  change (not a file in the repo), so it MUST be applied by whoever holds
  admin access to the GitHub repository as a manual follow-up if the
  implementing agent/tooling has no credentials to apply it directly —
  this feature's deliverable is the workflow the setting depends on, plus
  the documented instruction for applying the setting itself.
- **FR-005**: The system MUST provide a set of plain deployment manifests
  (Deployment, Service, Secret) describing how to run the app in a
  container orchestration cluster.
- **FR-006**: The system MUST provide an equivalent packaged chart form of
  the same deployment manifests, parameterized by environment-specific
  values (image location/tag, database connection details).
- **FR-007**: The deployment manifests and chart MUST inject the app's
  configuration via a config file path, matching how the app already reads
  configuration when run directly (not via new environment-variable-based
  configuration).
- **FR-008**: Applying the deployment manifests or installing the chart
  MUST remain a manual, deliberate action — this feature MUST NOT
  automatically trigger any deployment as part of the build check or any
  other automated process.
- **FR-009**: The deployment manifests MUST NOT include a real secret value
  committed to the repository — only a placeholder/example showing the
  expected shape.
- **FR-010**: The build check MUST measure code coverage and fail if
  overall coverage is below 74% (the enforced floor for this feature —
  see Clarifications; raise back toward the original 80% target in a
  follow-up pass as more of the codebase gets covered).
- **FR-011**: The build check MUST publish its coverage report to an
  external coverage-reporting service as part of the same CI run.
- **FR-012**: A failure to publish the coverage report to the external
  service MUST NOT, by itself, fail the build check — only the coverage
  threshold (FR-010) and the existing build/test results gate the merge.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of pushes to main and 100% of pull requests trigger an
  automated build check, with zero manual steps required to initiate it.
- **SC-002**: A maintainer can determine whether the latest change on main
  or on any open pull request builds successfully within the time it takes
  to look at the commit/PR status, with no need to check out and build
  locally to find out. Additionally, 0% of pull requests with a failing or
  incomplete build check can be merged into main.
- **SC-003**: A deployment can be performed (manifests applied or chart
  installed) using only the provided files plus environment-specific
  values, without needing to author any new Kubernetes resource definitions
  from scratch.
- **SC-004**: Zero automated deployments occur as a side effect of this
  feature — every deployment remains an explicit, manually-triggered action
  until a separate, future feature wires up continuous deployment.
- **SC-005**: 100% of build checks report an overall code coverage
  percentage, and any change that drops coverage below 74% fails the check
  and cannot be merged.
- **SC-006**: Coverage trends across changes are visible in an external
  coverage-reporting service, not only inside individual CI run logs.

## Assumptions

- "Main branch" refers to the repository's default branch, matching the
  existing branching convention already in use.
- No lint or separate deploy stage is introduced alongside the build check
  — per the roadmap, one job covers both triggers until a real need for a
  separate stage is demonstrated.
- The container image referenced by the deployment manifests/chart is
  expected to already exist (built and pushed to a registry) by the time
  someone actually applies these manifests — building and publishing that
  image to a registry is out of scope for this feature.
- No Ingress, horizontal pod autoscaling, or NetworkPolicy resources are
  included — per the roadmap, these are added only once the app actually
  needs to be reachable from outside the cluster, needs to scale, or needs
  network isolation.
- Continuous deployment (automatically applying these manifests on a
  release) is explicitly out of scope until a real cluster and image
  registry exist to deploy to.
- The coverage threshold (74%, per Clarifications) applies to overall
  project coverage (not a per-file or per-PR-diff minimum) — the simplest
  interpretation absent a more specific instruction.
- The coverage measurement and threshold enforcement run as part of the
  same single build check job (FR-003) — this doesn't introduce a separate
  CI stage, since it's naturally a step of the existing test/build task.
