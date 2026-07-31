# Feature Specification: Dependency Updates

**Feature Branch**: `009-dependency-updates`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 9"

## Clarifications

### Session 2026-07-31

- Q: Should bumps be capped at the current major version line, or always jump to the absolute latest major version even if that requires substantial rework? → A: Always jump to the absolute latest major version available, even if it requires substantial rework.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ship on current, supported dependency versions (Priority: P1)

As the team maintaining this API, we want the project's core dependencies
(language toolchain, web framework, ORM, database drivers, logging) to track
current stable upstream releases, so the app keeps receiving security fixes
and we don't accumulate a large, risky upgrade later.

**Why this priority**: Every phase's dependency risk compounds the longer an
upgrade is deferred; doing it now, in one deliberate pass, is far cheaper than
doing it under pressure after a vulnerability disclosure forces the issue.

**Independent Test**: Can be fully tested by running the full test suite
against the updated dependency set and confirming it passes with no
behavior change visible to API consumers.

**Acceptance Scenarios**:

1. **Given** the project currently pins a set of dependency versions,
   **When** each dependency is checked against its current stable upstream
   release, **Then** every dependency that has a newer stable release is
   bumped to that version.
2. **Given** the dependency versions have been bumped, **When** the full
   test suite is run, **Then** all tests pass with no source changes other
   than those required to accommodate breaking changes in the bumped
   dependencies.
3. **Given** a bumped dependency introduces a breaking change, **When** the
   change is identified, **Then** the minimal code change needed to restore
   passing behavior is made and covered by the existing test suite (or a new
   test, if the break reveals an untested path).

---

### User Story 2 - Single reviewable change set (Priority: P2)

As a reviewer, I want the dependency bump delivered as one pull request with
tests run once at the end, so I can review the full picture of what changed
and why, instead of reviewing dependency-by-dependency churn.

**Why this priority**: Keeps the change auditable and avoids noisy
intermediate commits that don't individually represent a working, tested
state.

**Independent Test**: Can be fully tested by inspecting the PR history for
this change and confirming it is a single PR with the test suite run once
covering the final, fully-bumped state (not once per dependency).

**Acceptance Scenarios**:

1. **Given** multiple dependencies need bumping, **When** the work is
   committed, **Then** it lands as one PR rather than one PR/commit per
   dependency.

---

### Edge Cases

- What happens when a dependency has no newer stable release available? It
  is left unchanged; this is not a failure of the feature, just a no-op for
  that dependency.
- How does the system handle a dependency bump that breaks the existing test
  suite? The bump for that dependency is still applied, but accompanied by
  the source changes needed to fix the break — the feature is not "done" if
  any test is left failing or skipped to route around the break.
- How does the system handle a dependency whose newer stable release
  requires a coordinated bump of a related dependency (e.g. a language
  version bump requiring a compatible framework version)? Both are bumped
  together as part of the same change, since neither is independently
  functional otherwise.
- How does the system handle a dependency whose latest stable release is a
  new major version with breaking changes? It is still bumped to that
  latest major version, and the rework needed to accommodate the breaking
  changes is done as part of this same change, however substantial —
  staying on an old major line is not an acceptable outcome.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST pin its language toolchain, web framework,
  ORM, database driver, and logging dependencies to their current stable
  upstream release versions at the time this feature is completed.
- **FR-002**: The full existing automated test suite MUST pass against the
  updated dependency set before the change is considered complete.
- **FR-003**: Any source change required solely to accommodate a breaking
  change in a bumped dependency MUST be included in this same change, not
  deferred.
- **FR-004**: The change MUST be delivered as a single reviewable unit
  (one PR), not as incremental per-dependency PRs or commits.
- **FR-005**: The change MUST NOT alter observable API behavior (request/
  response shapes, status codes, endpoint contracts) — this is a maintenance
  upgrade, not a feature or contract change.
- **FR-006**: The change MUST NOT introduce new runtime dependencies beyond
  what upgrading the existing ones requires (no unrelated library additions
  bundled into this pass).
- **FR-007**: Each dependency MUST be bumped to its absolute latest stable
  release, including crossing a major version boundary when one is
  available, rather than being capped at the currently-pinned major line.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the project's core dependencies (language toolchain,
  web framework, ORM, database drivers, logging) are on their current stable
  upstream release after this change.
- **SC-002**: 100% of the existing automated test suite passes after the
  change, with zero tests removed, skipped, or weakened to achieve that.
- **SC-003**: The change is delivered as exactly one pull request.
- **SC-004**: Zero observable changes to API request/response behavior for
  existing consumers.

## Assumptions

- "Current stable upstream release" means the latest non-pre-release
  (non-alpha/beta/RC) version published by each dependency's maintainer as
  of when this feature is implemented.
- The dependencies in scope are exactly those already version-pinned in the
  project's build configuration (language toolchain, web framework, ORM,
  Postgres driver, H2 test driver, logging) — no new dependencies are being
  evaluated for adoption as part of this feature.
- Breaking changes may range from minor/mechanical (e.g. API renames,
  deprecated-method replacements) to substantial (e.g. a major version
  crossing that changes core APIs) — per the Clarifications above, scope
  includes major version jumps, and the rework they require is part of
  this feature rather than a reason to defer the bump.
- No new automated tests are required beyond what's needed to cover any
  behavior that breaks and gets fixed as part of the bump (per FR-003) —
  this feature is not introducing new functionality to test.
