# Feature Specification: Search filter on statistics-meta

**Feature Branch**: `006-statistics-meta-search`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 6"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find a statistic by name or id substring (Priority: P1)

A client browsing `/statistics-meta` knows part of an entity's name or
`statistic_id` (e.g. "temperature") and wants only the matching entries
instead of scanning the full list.

**Why this priority**: Only story in this feature — the entire slice.

**Independent Test**: `curl "localhost:8080/statistics-meta?search=temperature"`
returns only entries whose `statistic_id` or `name` contains that text.

**Acceptance Scenarios**:

1. **Given** statistics-meta entries where some have "temperature" in
   `statistic_id` or `name` and some don't, **When** the client calls
   `GET /statistics-meta?search=temperature`, **Then** the response is 200
   with only the matching entries.
2. **Given** the same entries, **When** the client calls
   `GET /statistics-meta` with no `search` param, **Then** the response is
   200 with every entry (unchanged existing behavior).
3. **Given** a `search` value that matches nothing, **When** the client
   calls the endpoint, **Then** the response is 200 with an empty array.

### Edge Cases

- Empty `search` value (`?search=`): treated the same as no `search` param —
  returns every entry, not zero entries.
- `search` value only matches a `name` that is `null` for some rows: those
  rows are excluded, no error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept an optional `search` query parameter on
  `GET /statistics-meta`.
- **FR-002**: When `search` is present and non-empty, the system MUST
  return only entries whose `statistic_id` or `name` contains the given
  text as a substring, case-insensitively.
- **FR-003**: When `search` is absent or empty, the system MUST return
  every entry, unchanged from current behavior.
- **FR-004**: The substring match MUST be an OR across `statistic_id` and
  `name` — an entry matches if either field contains the text.
- **FR-005**: The search MUST be performed by the database query, not by
  fetching all rows and filtering in application code.

### Key Entities

- **Statistics-meta entry**: existing entity (`id`, `statistic_id`, `name`,
  `unit_of_measurement`, `source`, `has_mean`, `has_sum`); no new fields —
  `search` only narrows which rows are returned.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can narrow the statistics-meta list to only entries
  matching a text fragment in a single request, without downloading the
  full list first.
- **SC-002**: Omitting `search` continues to return the exact same result
  as before this feature shipped — no behavior change for existing callers.

## Assumptions

- Match is case-insensitive — friendlier default for a search feature than
  the case-sensitive substring match already used in `EventsRoutes`'s
  `contain` filter; no case-sensitivity requirement was stated.
- Single `search` param only (per roadmap wording — literally `?search=`),
  not the two-param `contain`/`exclude` shape from `EventsRoutes`; the
  roadmap's "same contain/exclude pattern" is read as "same substring-match
  code style," not "add an `exclude` param too." No `exclude` param — add
  one only if a real need for it shows up (YAGNI, matches the "not planned
  until triggered" framing this whole phase carries).
- No pagination added alongside search — out of scope per roadmap Phase 7,
  which gates pagination separately.
