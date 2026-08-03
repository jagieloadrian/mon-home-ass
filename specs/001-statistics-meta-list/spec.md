# Feature Specification: Statistic Entities List

**Feature Branch**: `001-statistics-meta-list`

**Created**: 2026-07-30

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 1"

## Clarifications

### Session 2026-07-30

- Q: When recorder DB is unreachable/query fails while serving the list, what should happen? → A: Explicit 503 — feature adds dedicated error handling: DB/query failure returns 503 Service Unavailable with a body, distinct from generic 500.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover trackable statistics (Priority: P1)

A client application (dashboard, reporting tool) needs to know which
statistics exist in the Home Assistant recorder database before it can
request any historical data for them.

**Why this priority**: Without this list, a client has no way to discover
what statistic IDs even exist — it's the entry point for every other
statistics feature.

**Independent Test**: Call the list endpoint against a recorder database
with known statistics loaded and confirm every one of them appears in the
response with its identifying and descriptive fields.

**Acceptance Scenarios**:

1. **Given** the recorder database has statistics configured (e.g. sensor
   temperature, energy usage), **When** a client requests the list, **Then**
   every configured statistic is returned with its statistic ID, display
   name, unit of measurement, source, and mean/sum capability flags.
2. **Given** a statistic entry has no display name or unit set in the
   source data, **When** it is returned, **Then** those fields are present
   with a null value rather than being omitted or causing an error.

---

### Edge Cases

- What happens when the recorder database has zero statistics configured?
  Endpoint returns an empty list, not an error.
- How does the system handle statistic entries with null optional fields
  (name, unit of measurement, source, mean/sum capability flags)? Returned
  as null, passed through as-is.
- What happens when the recorder database is unreachable or the query
  fails? Client receives an explicit 503 Service Unavailable with an error
  body, distinguishable from a generic server error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a way to retrieve the full list of
  trackable statistics known to the recorder database.
- **FR-002**: Each returned statistic MUST include: its unique identifier,
  its statistic ID (external/business identifier), display name, unit of
  measurement, data source, and whether it supports mean/sum aggregation.
- **FR-003**: System MUST return an empty list (not an error) when no
  statistics are configured in the source database.
- **FR-004**: System MUST NOT modify the recorder database in any way while
  serving this list — the underlying access is read-only.
- **FR-005**: Optional/unset fields on a statistic (name, unit, source,
  mean/sum flags) MUST be represented as null rather than omitted or
  defaulted to a misleading value.
- **FR-006**: System MUST return a 503 Service Unavailable response with an
  error body when the recorder database is unreachable or the underlying
  query fails, distinct from an unhandled/generic 500.

### Key Entities *(include if feature involves data)*

- **Statistic Metadata**: Describes one trackable statistic from the Home
  Assistant recorder — its identifier, human-readable name, unit of
  measurement, originating source, and which aggregations (mean, sum) it
  supports. Sourced from the recorder's statistics metadata table; this
  feature only reads it, never writes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can retrieve the complete list of trackable
  statistics in a single request, with no need for prior knowledge of what
  exists.
- **SC-002**: 100% of statistics present in the source recorder database
  are represented in the response — none silently dropped.
- **SC-003**: The list reflects newly added recorder statistics without any
  code change — it is fully data-driven.
- **SC-004**: For a typical single-household installation (tens to a few
  hundred statistics), the list is returned in under 1 second.

## Assumptions

- Readonly access is a hard requirement, not just a default — see FR-004.
- No search, filter, or pagination is needed yet: the list is small (one
  row per configured HA statistic), so the full set is returned every call.
  (Tracked as a separate, trigger-gated roadmap phase if that changes.)
- No authentication/authorization is required beyond what the rest of the
  API already has (none) — consistent with existing recorder/migration
  endpoints.
