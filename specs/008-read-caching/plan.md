# Implementation Plan: Read caching layer

**Branch**: `feature-3/add-more-endpoints` | **Date**: 2026-07-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-read-caching/spec.md`

## Summary

Every existing read repo method (`StatisticMetaRepo.getAll`,
`StatisticRepo.getLongTerm`/`getShortTerm`/`getSummary`,
`StateMetadataRepo.getAll`) wraps its database query in a Redis-backed
cache (`redis.clients:jedis`), keyed per-method with a 5-minute TTL. No
new endpoint, no response-shape change — purely an internal performance
layer (FR-004: transparent to clients). Test infrastructure moves from
H2/ambient-Docker to Testcontainers for both Postgres and Redis, per
explicit user direction during implementation planning (supersedes the
originally analyzed in-process `ConcurrentHashMap` decision — see
`research.md`).

## Technical Context

**Language/Version**: Kotlin (existing project toolchain)

**Primary Dependencies**: `redis.clients:jedis:7.5.3` (new, runtime — Maven Central, checked 2026-07-31); `org.testcontainers:testcontainers:1.21.4` + `org.testcontainers:postgresql:1.21.4` (new, test-only — Maven Central, checked 2026-07-31)

**Storage**: Redis for the cache itself (new); still reads from Postgres (readonly recorder DB) on a cache miss — both Postgres and Redis are now Testcontainers-managed in tests, not H2/ambient containers

**Testing**: Kotlin test — a new unit test for `RedisCache` itself (TTL expiry via short millisecond TTL, null-skip, against the Testcontainers Redis instance), plus additions to each existing route test class proving cache-hit behavior (stale value survives a direct DB mutation within the TTL); every existing DB-touching test file's setup moves off H2 onto the shared Testcontainers Postgres instance

**Target Platform**: Linux server (existing deployment); Docker required wherever the test suite runs (new constraint — see research.md "Consequences worth naming plainly")

**Project Type**: Single Ktor web-service project (existing structure, no new module)

**Performance Goals**: Cache hits skip the Postgres round-trip entirely (SC-001); this is the feature's whole point

**Constraints**: Readonly DB only (Principle I, unaffected — cache sits in front of existing read-only queries); no cache invalidation API — time-based expiry only; Redis becomes a new single point of failure for previously-DB-only-dependent reads (documented in data-model.md, no fallback built — not requested)

**Scale/Scope**: One new `RedisCache` utility class, one new unit test file, a rewritten `TestDatabase.kt` (Testcontainers-backed), 4 existing repo classes each gain a cache wrap, 5 existing route test files gain a cache-hit case and move off H2, new `redis` config section in both `application.yaml` and `application-test.yaml`; no route/DTO/openapi changes (FR-004)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Readonly Data Boundary**: PASS — cache only wraps existing `SELECT` calls; no new query shape, no writes.
- **II. YAGNI / Minimal Diff**: **DEVIATION, justified below.** Adding Jedis + Testcontainers is more than the roadmap's own stated floor ("even an in-memory `ConcurrentHashMap` with TTL") and more than Constitution Principle II's "prefer stdlib" default would produce on its own. Justification: explicit, repeated user direction during `/speckit-implement` (confirmed via clarifying questions on both the Redis choice and the Testcontainers scope) — not a silent deviation. A Redis instance already exists in this environment, so the infrastructure cost is real but not novel-to-provision. Also note: roadmap flags this whole phase as trigger-gated ("do NOT build until a profiler shows the bottleneck") — proceeding on explicit user request, not because the trigger fired (same pattern as Phases 6/7).
- **III. Token-Efficient Execution**: PASS — plan produced under ponytail+caveman discipline; ponytail comments mark the two deliberate ceilings (no eviction cap, no Redis-down fallback) rather than building unrequested resilience.
- **IV. Working App After Every Implementation**: Applies at `/speckit-implement` time — tracked as a task. Note: "working" now additionally requires Docker to be available for the test suite (new constraint vs. H2's zero-dependency default).
- **V. Test-Backed Changes**: PASS (planned) — new `RedisCache` utility gets a dedicated unit test; existing route test classes get cache-behavior additions; existing DB test setup is migrated (not dropped), so prior coverage is preserved on the new infrastructure.

Also note: the Constitution's **Technology Constraints** section names "H2 for tests" as part of the fixed stack — moving to Testcontainers-managed Postgres is a documented deviation from that section too, per the same explicit user direction. This is flagged here rather than silently done; a formal constitution amendment (version bump) is a separate, out-of-band action if the maintainer wants the doc itself updated to match.

## Project Structure

### Documentation (this feature)

```text
specs/008-read-caching/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

No `contracts/` — no new endpoint, no response-shape/status-code change
(FR-004: transparent to clients), so there is no new interface contract to
document.

### Source Code (repository root)

```text
src/main/kotlin/com/anjo/monhomeass/
├── cache/RedisCache.kt                 # NEW — generic getOrPut(key, loader) over Jedis, PSETEX-based TTL, skips caching null
├── plugins/DependencyInjection.kt      # EXTEND — provide JedisPooled + RedisCache, read redis.* config
├── repository/StatisticMetaRepo.kt     # EXTEND — getAll wraps its query in RedisCache
├── repository/StatisticRepo.kt         # EXTEND — getLongTerm/getShortTerm/getSummary each wrap their query
└── repository/StateMetadataRepo.kt     # EXTEND — getAll wraps its query

src/main/resources/
├── application.yaml                     # EXTEND — new redis: host/port/ttlMillis section
└── application-test.yaml                # UNUSED for DB/redis config once Testcontainers config is injected per-test (kept for any other unrelated config keys)

src/test/kotlin/com/anjo/monhomeass/
├── testutils/TestDatabase.kt                          # REWRITE — Testcontainers Postgres + Redis, shared singletons, testApplicationConfig() helper, flushRedis()
├── cache/RedisCacheTest.kt                             # NEW — TTL expiry, per-key isolation, null-skip (against Testcontainers Redis)
├── routing/StatisticsMetaRoutesKtTest.kt               # EXTEND — cache-hit case; migrate off ApplicationConfig(file) to testApplicationConfig()
├── routing/StatisticLongTermRoutesKtTest.kt            # EXTEND — same
├── routing/StatisticShortTermRoutesKtTest.kt           # EXTEND — same
├── routing/StatisticSummaryRoutesKtTest.kt             # EXTEND — same
└── routing/EntitiesRoutesKtTest.kt                     # EXTEND — same

build.gradle.kts                        # EXTEND — add jedis (implementation) + testcontainers/testcontainers-postgresql (testImplementation)
```

**Structure Decision**: Single existing Ktor project, no new module. The
cache wrap stays additive inside existing repo methods (FR-004
transparency, unchanged from the original plan); the larger change is the
test infrastructure swap, which touches every existing DB-backed test
file mechanically (same assertions, different connection setup) rather
than changing any test's actual behavior or expectations.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| New runtime dependency (Jedis) + new test dependency (Testcontainers) | Explicit, confirmed user direction overriding the roadmap's stdlib-first floor | The simpler alternative (in-process `ConcurrentHashMap`, H2 for tests) was the original plan that passed `/speckit-analyze` cleanly — rejected only because the user explicitly asked for Redis + Testcontainers instead, twice, after being shown the tradeoffs |
