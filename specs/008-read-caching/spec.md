# Feature Specification: Read caching layer

**Feature Branch**: `008-read-caching`

**Created**: 2026-07-31

**Status**: Draft

**Input**: User description: "@user-roadmap.md phase 8"

## Clarifications

### Session 2026-07-31

- Q: Which GET endpoints are in scope for caching? → A: All read GET endpoints — `statistics-meta` (incl. `search`), `/statistics/{statisticId}` and its `/short-term` variant (incl. `from`/`to`/`limit`/`offset`), `/statistics/{statisticId}/summary`, and `/entities`.
- Q: Cache freshness window (TTL)? → A: 5 minutes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Repeated reads skip the database round-trip (Priority: P1)

A client (or several clients) repeatedly requests the same read-only data
— e.g. the statistics-meta list, or the same entity's statistic range —
within a short window. Today every request re-hits Postgres even though
the underlying recorder data barely changes between calls.

**Why this priority**: Only story in this feature — the entire slice.

**Independent Test**: Call the same cached endpoint twice in quick
succession; the second call returns the identical result without a new
database query being issued (observable via logs/query count in a test).

**Acceptance Scenarios**:

1. **Given** a cached endpoint was called once, **When** the same request
   (same path + query params) is repeated within the cache's freshness
   window, **Then** the response is served from cache — identical body, no
   new database query.
2. **Given** the cache's freshness window has elapsed, **When** the same
   request is repeated, **Then** the system queries the database again and
   refreshes the cached value.
3. **Given** two different requests (different `statisticId`, or different
   `from`/`to`/`search`/`limit`/`offset` query params), **When** both are
   called, **Then** each is cached independently — one does not serve the
   other's result.

### Edge Cases

- Underlying recorder database is unreachable: a cache miss still surfaces
  the existing 503 behavior; a cache hit is served regardless (stale-but-
  available takes priority over a hard failure for reads within the
  freshness window).
- Recorder data actually changes between two calls within the freshness
  window: the client may see the old value until the window elapses — an
  accepted tradeoff of caching, not a bug.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST cache the response of every read GET endpoint:
  `GET /statistics-meta` (with or without `search`), `GET
  /statistics/{statisticId}` and its `/short-term` variant (with any
  `from`/`to`/`limit`/`offset` combination), `GET
  /statistics/{statisticId}/summary`, and `GET /entities`.
- **FR-002**: Each distinct request (path + full query-parameter set) MUST
  be cached and served independently — no cross-contamination between
  different `statisticId`/`search`/`from`/`to`/`limit`/`offset` combinations.
- **FR-003**: A cached entry MUST expire 5 minutes after it was populated,
  after which the next request re-queries the database and refreshes the
  cache.
- **FR-004**: Cache behavior MUST be transparent to clients — response
  body and status codes are unchanged whether served from cache or from a
  fresh database query.
- **FR-005**: System MUST NOT cache error responses (400/404/503) — only
  successful reads are cached, so a transient failure doesn't get "stuck"
  for the freshness window.

### Key Entities

- **Cache entry**: a stored (request key → response) pair with an
  expiration time; no persistence beyond process memory, no new database
  table.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Repeating the same read request within the freshness window
  produces zero additional database round-trips for that request.
- **SC-002**: Response content for a cached endpoint is identical whether
  served from cache or freshly queried — no behavior change visible to
  clients.
- **SC-003**: Memory used by cached entries stays bounded — it does not
  grow unbounded as distinct query combinations accumulate over the life
  of the process.

## Assumptions

- This whole feature stays trigger-gated per the roadmap ("do NOT build
  until a profiler shows the readonly Postgres round-trip is actually the
  bottleneck") — spec'd per explicit user request, not because the
  trigger fired.
- No cache invalidation API — the DB is externally managed (Home
  Assistant recorder) and readonly from this app's side, so there's no
  write path to invalidate from; expiration is time-based only.
- Roadmap's own suggested floor was an in-process cache ("Caffeine or even
  an in-memory `ConcurrentHashMap` with TTL"); superseded during planning
  by explicit user direction to use Redis instead — see `research.md` for
  the resulting mechanism/testing tradeoffs (new runtime dependency, tests
  now require a live Redis instance).
