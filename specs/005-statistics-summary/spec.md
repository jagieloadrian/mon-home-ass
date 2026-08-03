# Feature Specification: Statistics date-range summary

**Feature Branch**: `005-statistics-summary`

**Created**: 2026-07-30

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 5"

## Clarifications

### Session 2026-07-30

- Q: `statistics` rows already store per-period `min`/`mean`/`max`/`sum`/`state`. Which columns should the overall summary aggregate? → A: Aggregate everything from the `state` column only: MIN(state), MAX(state), AVG(state), SUM(state)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Get aggregate stats for a date range (Priority: P1)

A client already knows a `statisticId` (e.g. from `/statistics-meta`) and
wants a single overall summary (min/max/avg/sum) for a period, instead of
fetching every row and reducing it themselves.

**Why this priority**: Only story in this feature — the entire slice.

**Independent Test**: `curl "localhost:8080/statistics/{statisticId}/summary?from=...&to=..."`
returns one JSON object with aggregate values for that entity/range.

**Acceptance Scenarios**:

1. **Given** a known `statisticId` with rows inside `[from, to]`, **When** the
   client calls `GET /statistics/{statisticId}/summary?from&to`, **Then** the
   response is 200 with one object containing overall min/max/avg/sum across
   those rows.
2. **Given** a known `statisticId` with no rows inside `[from, to]`, **When**
   the client calls the endpoint, **Then** the response is 200 with an object
   whose numeric fields are `null` (nothing to aggregate).
3. **Given** an unknown `statisticId`, **When** the client calls the
   endpoint, **Then** the response is 404.
4. **Given** `from`/`to` are omitted, **When** the client calls the
   endpoint, **Then** the summary covers the entity's full history (no range
   filter applied).
5. **Given** `from`/`to` are present but unparseable, **When** the client
   calls the endpoint, **Then** the response is 400.
6. **Given** the recorder database is unreachable or the query fails,
   **When** the client calls the endpoint, **Then** the response is 503.

### Edge Cases

- `from` after `to`: treated as a normal (empty-result) range — same as
  scenario 2, not an error.
- `statisticId` exists but has zero rows ever recorded: same as scenario 2.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /statistics/{statisticId}/summary`
  returning one aggregate object for the given entity.
- **FR-002**: System MUST accept optional `from`/`to` query parameters
  (same accepted formats as the existing `/statistics/{statisticId}`
  endpoint: epoch seconds or ISO instant) to bound the range.
- **FR-003**: System MUST compute min, max, average, and sum over the
  `state` values of matching rows.
- **FR-004**: System MUST return 404 when `statisticId` has no matching
  entry in statistics metadata (same behavior as the existing long-term
  endpoint).
- **FR-005**: System MUST return 400 when `from` or `to` is present but not
  parseable (same behavior as the existing long-term endpoint).
- **FR-006**: System MUST return 503 when the recorder database is
  unreachable or the query fails.
- **FR-007**: System MUST return 200 with null aggregate fields (not 404)
  when the range contains zero rows for an otherwise-known `statisticId`.
- **FR-008**: Aggregation MUST be computed by the database (SQL aggregates),
  not by fetching all matching rows and reducing them in application code.
- **FR-009**: System MUST source rows from the long-term `statistics` table
  only (short-term is out of scope for this feature).
- **FR-010**: System MUST echo the effective `from`/`to` range in the
  response as ISO-8601 instant strings, `null` when that bound was omitted
  from the request.

### Key Entities

- **Statistics summary**: derived (non-persisted) aggregate over a
  `statisticId`'s `statistics` rows within an optional date range — min,
  max, average, and sum of `state` — plus the effective `from`/`to` range
  echoed back (`null` when omitted).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can get a min/max/avg/sum summary for any known
  entity's full history or any custom date range in a single request.
- **SC-002**: Summary requests return in roughly the same time as a single
  `/statistics/{statisticId}` row lookup, regardless of how many rows fall
  inside the range (aggregation happens in the database, not by transferring
  every row to the client).
- **SC-003**: Every response for a known `statisticId` is a well-formed
  summary object, even when the range matches no rows.

## Assumptions

- "Overall aggregate" means the 4 SQL aggregates already named in the
  roadmap (min/max/avg/sum) — no percentiles, no bucketed/grouped summaries.
- Only the long-term `statistics` table is summarized here, matching the
  roadmap's phase wording ("over `statistics`"); a short-term summary
  variant is not requested and is out of scope.
- `statisticId` existence/404 semantics mirror the existing
  `/statistics/{statisticId}` endpoint exactly, for consistency.
- No new query params (e.g. grouping) beyond `from`/`to` — matches roadmap
  scope precisely (YAGNI).
