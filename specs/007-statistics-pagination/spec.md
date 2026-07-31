# Feature Specification: Pagination on long/short-term statistics

**Feature Branch**: `007-statistics-pagination`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 7"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fetch one page of a large statistic range (Priority: P1)

A client querying `/statistics/{statisticId}` (or its `/short-term`
variant) over a wide date range wants only a page of rows at a time,
instead of the entire range in one response.

**Why this priority**: Only story in this feature — the entire slice.

**Independent Test**: `curl "localhost:8080/statistics/{statisticId}?limit=10&offset=20"`
returns exactly the 10 rows starting after the first 20, in the existing
oldest-first order.

**Acceptance Scenarios**:

1. **Given** a known `statisticId` with more rows than `limit`, **When** the
   client calls `GET /statistics/{statisticId}?limit=N`, **Then** the
   response is 200 with exactly the first `N` rows, oldest-first.
2. **Given** the same data, **When** the client adds `&offset=M`, **Then**
   the response is 200 with the `N` rows starting after the first `M`,
   same ordering.
3. **Given** `limit`/`offset` are omitted, **When** the client calls the
   endpoint, **Then** the response is every matching row — unchanged from
   current behavior.
4. **Given** `offset` is at or beyond the total number of matching rows,
   **When** the client calls the endpoint, **Then** the response is 200
   with an empty array.
5. **Given** `limit` or `offset` is negative, **When** the client calls the
   endpoint, **Then** the response is 400.
6. **Given** the `/short-term` variant, **When** the client applies the
   same `limit`/`offset` params, **Then** it behaves identically to the
   long-term endpoint's pagination.

### Edge Cases

- `limit=0`: valid, not an error — returns an empty array (zero rows
  requested).
- `limit`/`offset` combined with existing `from`/`to` range params: the
  range filter is applied first, pagination slices the filtered result.
- Non-numeric `limit`/`offset` value: treated the same as a negative value
  — 400.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept optional `limit`/`offset` query
  parameters on `GET /statistics/{statisticId}` and its `/short-term`
  variant.
- **FR-002**: When both are absent, the system MUST return every matching
  row, unchanged from current behavior.
- **FR-003**: When present, the system MUST return at most `limit` rows,
  skipping the first `offset` rows, preserving the existing oldest-first
  ordering.
- **FR-004**: The system MUST apply `limit`/`offset` after any `from`/`to`
  range filtering, not instead of it.
- **FR-005**: The system MUST return 400 when `limit` or `offset` is
  present but negative or non-numeric.
- **FR-006**: The system MUST return 200 with an empty array (not an
  error) when `offset` is at or beyond the number of matching rows.
- **FR-007**: Pagination MUST be performed by the database query (SQL
  `LIMIT`/`OFFSET`), not by fetching every matching row and slicing in
  application code.
- **FR-008**: Both `GET /statistics/{statisticId}` and
  `GET /statistics/{statisticId}/short-term` MUST support `limit`/`offset`
  identically.

### Key Entities

- No new entity — `limit`/`offset` narrow which rows of the existing
  long-term/short-term statistic rows are returned; no new fields.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can retrieve a bounded page of a large statistic
  range in a single request, without downloading the entire range first.
- **SC-002**: Omitting `limit`/`offset` continues to return the exact same
  result as before this feature shipped — no behavior change for existing
  callers.
- **SC-003**: Response size for a paginated request stays proportional to
  `limit`, regardless of how large the underlying matching range is.

## Assumptions

- No maximum cap enforced on `limit` — roadmap doesn't call for one, and
  this is an internal/readonly API with no public rate-limit concern
  today; add a cap only if a real need for one shows up (YAGNI).
- No response envelope (total count, has-more flag, etc.) — response stays
  a plain array, matching every existing endpoint's shape; add metadata
  only if a client actually needs to know the total without a second call.
- Same `limit`/`offset` semantics for both the long-term and short-term
  endpoints — no divergent behavior between the two variants.
- This whole feature stays trigger-gated per the roadmap ("do NOT build
  until a real result set is too big to return in one response") — spec'd
  per explicit user request, not because the trigger fired.
