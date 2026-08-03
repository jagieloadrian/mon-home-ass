# Feature Specification: Short-term (5-minute) statistics for one entity

**Feature Branch**: `003-statistics-short-term`

**Created**: 2026-07-30

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 3"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Retrieve short-term statistics for a known entity (Priority: P1)

A client integrating with the monitoring API wants recent high-resolution
(5-minute interval) recorded values (mean/min/max/sum/state per period) for
one Home Assistant statistic, so it can chart or analyze that entity's
recent short-term history.

**Why this priority**: Core value of the feature — without it there is
nothing to deliver. Short-term data complements Phase 2's long-term data
for fine-grained recent analysis.

**Independent Test**: Can be fully tested by calling
`GET /statistics/{statisticId}/short-term` for a `statisticId` known to
exist (via Phase 1's `/statistics-meta` endpoint) and confirming the
returned rows.

**Acceptance Scenarios**:

1. **Given** a valid, existing `statisticId`, **When** a client calls
   `GET /statistics/{statisticId}/short-term`, **Then** the response is a
   200 with a JSON array of short-term statistic rows for that entity.
2. **Given** a valid `statisticId`, **When** the client omits `from`/`to`,
   **Then** the response returns all available short-term rows for that
   entity (no date filtering applied).

---

### User Story 2 - Filter by date range (Priority: P2)

A client wants only the short-term rows within a specific time window, so
it isn't forced to fetch and filter the full high-resolution history
client-side.

**Why this priority**: Useful and named explicitly in the roadmap (same
`from`/`to` contract as Phase 2), but the endpoint delivers value even
without it (US1 covers the unfiltered case).

**Independent Test**: Can be tested independently by calling the endpoint
with `from`/`to` query params and confirming only in-range rows return.

**Acceptance Scenarios**:

1. **Given** a valid `statisticId` and a `from`/`to` range, **When** the
   client calls the endpoint, **Then** only rows with a period start within
   `[from, to]` are returned.
2. **Given** only `from` is supplied (no `to`), **When** the client calls
   the endpoint, **Then** all rows from `from` onward are returned.
3. **Given** only `to` is supplied (no `from`), **When** the client calls
   the endpoint, **Then** all rows up to `to` are returned.

---

### User Story 3 - Unknown statistic returns a clear error (Priority: P3)

A client that queries a `statisticId` that doesn't exist should get an
unambiguous "not found" response instead of an empty array indistinguishable
from "exists but no data".

**Why this priority**: Correctness/UX polish — matches Phase 2's behavior
for consistency — but the feature is usable without it.

**Independent Test**: Call the endpoint with a `statisticId` not present in
`statistics_meta` and confirm a 404 is returned.

**Acceptance Scenarios**:

1. **Given** a `statisticId` not present in the system, **When** the client
   calls `GET /statistics/{statisticId}/short-term`, **Then** the response
   is 404.

---

### Edge Cases

- Valid `statisticId` with zero rows in range (entity exists, no short-term
  data for that period): returns 200 with an empty array, not 404.
- `from` after `to`: returns an empty array rather than erroring.
- Malformed `from`/`to` values (unparseable date/epoch): returns a 400.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /statistics/{statisticId}/short-term`
  returning short-term statistic rows (mean, min, max, sum, state, period
  start) for the given statistic — same row shape as Phase 2's long-term
  endpoint.
- **FR-002**: System MUST resolve `statisticId` to the underlying metadata
  record and return only short-term rows belonging to that entity.
- **FR-003**: System MUST support optional `from` and `to` query parameters
  to bound results by period start; when omitted, no bound is applied on
  that side. Each accepts either an epoch-seconds integer or an ISO-8601
  date/time string (same contract as Phase 2).
- **FR-004**: System MUST return 404 when `statisticId` does not correspond
  to any known statistic.
- **FR-005**: System MUST return 400 when `from` or `to` is present but is
  neither a valid epoch-seconds integer nor a valid ISO-8601 string.
- **FR-006**: System MUST NOT perform any write/update/delete against the
  recorder database — this endpoint is read-only.

### Key Entities *(include if feature involves data)*

- **Short-term statistic row**: A single recorded high-resolution (5-minute
  interval) statistic sample for one entity — period start, mean, min, max,
  sum, and state values. Same shape as Phase 2's long-term statistic row.
- **Statistic metadata**: Existing entity (from Phase 1) that a short-term
  row belongs to, resolved via `statisticId`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can retrieve an entity's full short-term history in
  a single request.
- **SC-002**: A client can narrow short-term results to a specific date
  range without retrieving unrelated periods.
- **SC-003**: A client can distinguish "unknown entity" (404) from "entity
  exists but no short-term data in range" (200, empty array) without
  ambiguity.

## Assumptions

- `statisticId` in the URL refers to the same identifier already resolved
  by Phase 1's `/statistics-meta` endpoint, matching Phase 2's usage.
- `from`/`to` accept either epoch seconds or an ISO-8601 string, identical
  to Phase 2's resolved contract (see Phase 2's Clarifications).
- Response row shape is identical to Phase 2's long-term statistic row
  (mean/min/max/sum/state/period start) — no separate schema, per the
  roadmap's explicit YAGNI note ("one DTO covers both").
- Row ordering is oldest-first by period start, matching Phase 2.
- No pagination — consistent with the roadmap's YAGNI stance (Phase 7 is
  explicitly gated on a real need and covers both long-term and short-term).
