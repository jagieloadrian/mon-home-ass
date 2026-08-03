# Feature Specification: Entities list

**Feature Branch**: `004-entities-list`

**Created**: 2026-07-30

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 4"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover known entities (Priority: P1)

A client integrating with the monitoring API wants to know which Home
Assistant entities exist, so it can figure out which `entity_id`s are
available before querying anything else about them.

**Why this priority**: This is the entire feature — a simple discovery
list with no variants or filters. Without it there is nothing to deliver.

**Independent Test**: Can be fully tested by calling `GET /entities` and
confirming the response contains the known entity ids.

**Acceptance Scenarios**:

1. **Given** entities exist in the system, **When** a client calls
   `GET /entities`, **Then** the response is a 200 with a JSON array of
   entity entries, one per known entity.
2. **Given** no entities exist in the system, **When** a client calls
   `GET /entities`, **Then** the response is a 200 with an empty array.

---

### Edge Cases

- Recorder database unreachable or query fails: returns a 503 (matches the
  existing behavior of `/statistics-meta` and other endpoints in this API).
- An entity record with a missing/null `entity_id`: included in the
  response with that field null, not silently dropped (matches the existing
  null-tolerant pattern used by `/statistics-meta`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose `GET /entities` returning the list of
  known entities (their `entity_id`).
- **FR-002**: System MUST return a 200 with an empty array when no entities
  exist, not an error.
- **FR-003**: System MUST NOT perform any write/update/delete against the
  recorder database — this endpoint is read-only.
- **FR-004**: System MUST return a 503 when the recorder database is
  unreachable or the query fails.

### Key Entities *(include if feature involves data)*

- **Entity**: A known Home Assistant entity — identified by `entity_id`.
  Read-only reference data used by clients to discover what they can query
  statistics for.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client can retrieve the full list of known entities in a
  single request.
- **SC-002**: A client can use the returned entity ids directly as input to
  other endpoints in this API without additional lookups.

## Assumptions

- No filtering, search, or pagination for this phase — the list is
  small (one row per known entity) and the roadmap explicitly gates
  search/pagination behind a future trigger (Phase 6/7), not needed here.
- Response shape mirrors the existing `/statistics-meta` list pattern: a
  flat JSON array, no envelope/wrapper object, no `id` field is present in
  the source table beyond `entity_id` itself.
