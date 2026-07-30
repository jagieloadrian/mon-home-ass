# Roadmap: Statistics API

Goal: expose Home Assistant recorder stats (readonly DB user) as HTTP endpoints.
Schema source: `notatki.txt`. Existing pattern already in repo (table object →
DTO → repo → route, registered in `Routing.kt`, described for swagger/openapi) —
every phase below just repeats that pattern for new tables. No new
abstraction layer needed.

## Phase 1 — Statistic entities list
`GET /statistics-meta` — list trackable statistics (id, statistic_id, name,
unit_of_measurement, source, has_mean, has_sum). Backed by `StatisticMetaData`
table (already modeled). New: `StatisticMetaDataDto`, `StatisticMetaRepo`,
`statisticMetaRoute`.

## Phase 2 — Long-term statistics for one entity
`GET /statistics/{statisticId}` — rows from `statistics` table
(mean/min/max/sum/state per period) for a given `statistic_id`, joined
through `statistics_meta` to resolve `metadata_id`. Query params: `from`,
`to` (epoch seconds or ISO, reuse `epochSeconds` column type already in
codebase). New: `StatisticDto`, `StatisticRepo`, `statisticRoute`.

## Phase 3 — Short-term (5-min) statistics for one entity
`GET /statistics/{statisticId}/short-term` — same shape as Phase 2, backed
by `statistics_short_term`. Reuse `StatisticDto` (identical columns), just a
different repo method hitting `StatisticShortTerm` table.

skipped: separate DTO for short-term — same fields as long-term, one DTO
covers both. Add a distinct one only if the shapes actually diverge.

## Phase 4 — Entities list (states_meta)
`GET /entities` — list of known `entity_id`s from `states_meta`. Small,
useful for clients to discover what they can query stats for. New:
`EntityDto`, `StateMetadataRepo`, `entitiesRoute`.

## Phase 5 — Date-range summary per entity
`GET /statistics/{statisticId}/summary?from&to` — single aggregate row
(overall min/max/avg/sum across the range) computed with SQL
aggregates (`MIN`/`MAX`/`AVG`/`SUM`) over `statistics`, not fetched-then-reduced
in Kotlin. New DTO `StatisticSummaryDto`, one repo method.

## Phase 6 — Search filter on statistics-meta (trigger-gated)
`?search=` on `GET /statistics-meta`, substring match on `statistic_id`/`name`,
same contain/exclude pattern already used in `EventsRoutes`/`EventService`.

Do NOT build until: a client actually needs to filter the list (list is
small — one row per HA statistic — full list is fine until then).

## Phase 7 — Pagination on Phase 2/3 (trigger-gated)
`limit`/`offset` query params on `GET /statistics/{statisticId}` and its
short-term variant.

Do NOT build until: a real result set is actually too big to return in one
response. `recorder`/`migration` endpoints don't paginate either — same bar
applies here.

## Phase 8 — Caching layer (trigger-gated)
Cache repeated reads (e.g. `statistics-meta`, hot statistic ranges) instead
of hitting Postgres every call.

Do NOT build until: a profiler shows the readonly Postgres round-trip is
actually the bottleneck. Then reach for the simplest thing that covers it
(e.g. Caffeine or even an in-memory `ConcurrentHashMap` with TTL) — no
framework-y cache abstraction.

## Phase 9 — Dependency updates
Bump `gradle.properties` versions to latest stable:
`kotlin_version` (2.3.0), `ktor_version` (3.4.0), `exposed_version` (1.0.0),
`postgres_version` (42.7.2), `h2_version` (2.2.224), `logback_version`
(1.5.13) — check each against current upstream release, bump, run full
test suite, fix any breakage before merging. One PR, run tests once at the
end — not one commit per dependency.

## Not planned
- write endpoints — DB user is readonly, no requirement exists for this.

## Phase 10 — CI build/PR check
`.github/workflows/build.yml` — `./gradlew build` on push to `main` and on
every PR. One job covers both triggers, no separate lint/deploy stage until
one is actually needed.

## Phase 11 — Deploy scaffold (not wired up yet)
`.devops/k8s` (plain manifests) + `.devops/helm/mon-home-ass` (chart) —
Deployment/Service/Secret for the app, config injected via
`-config=/config/application.yaml`. Manual `kubectl apply`/`helm install`
for now; add a CD workflow step only once there's a real cluster/registry
to deploy to.

## Contract
`openapi.yaml` (repo root) — hand-written OpenAPI 3.0 contract for the 5
phases above. Ktor already auto-generates a routing-based OpenAPI doc via
`ktor-server-routing-openapi` (see `Routing.kt` `swaggerUI`/`OpenApiDocSource.Routing`) —
this file is the upfront design contract to build against; once phases 1-5
ship, the live `/swagger` output should match it (diff them, don't
hand-maintain two sources of truth long-term).
