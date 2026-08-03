# Research: Read caching layer

## Decision: Cache mechanism — Redis (supersedes initial ConcurrentHashMap decision)

**Decision**: `redis.clients:jedis:7.5.3` (Maven Central, checked
2026-07-31) against a Redis instance, wrapped in a small `RedisCache`
utility class (`getOrPut(key, loader)`, not a framework-y abstraction).

**Rationale**: Explicit user direction during `/speckit-implement`
overrides the roadmap's own suggested floor and the original in-process
`ConcurrentHashMap` decision below. A Redis instance is already running
in this environment (`my-redis` container, port 6379), so the
"single-instance deployment, no need for a shared cache" argument from the
original decision no longer holds as a blocker — the infrastructure
exists and the user wants it used.

**Original decision (superseded)**: Plain `ConcurrentHashMap`-backed
`TtlCache<K, V>`, hand-rolled, no new dependency — kept here for the
record since it's what Constitution Principle II (prefer stdlib) and the
roadmap's own wording would otherwise have produced. Superseding it is a
documented deviation, not a silent one.

**Alternatives considered for the client library**:
- **Lettuce**: reactive/Netty-based, async-first API. Rejected — the rest
  of this codebase is synchronous/blocking (JDBC via Exposed
  `transaction {}`); Jedis's blocking API matches that style with far less
  ceremony.
- **Caffeine / plain `ConcurrentHashMap`**: see original decision above —
  no longer the direction, but would have been the smaller-diff, no-new-
  dependency choice if Redis hadn't been explicitly requested.

## Decision: Cache key strategy

**Decision**: One string key per call, built from a fixed per-method
prefix plus the method's own parameters (e.g.
`"statistics:long-term:$statisticId:$from:$to:$limit:$offset"`), value
serialized to JSON via the existing `kotlinx.serialization` setup already
used for DTOs.

**Rationale**: Redis keys are strings, not structural objects, so (unlike
the original `List<Any?>` idea for an in-process map) each method needs an
explicit prefix to avoid collisions with other methods sharing the same
Redis keyspace. JSON serialization reuses the exact `@Serializable` DTOs
already in place — no new serialization mechanism.

## Decision: Where the wrap lives

**Decision**: Inside each repo method body, wrapping only the existing
query logic (not the route handler) — unchanged from the original plan.

**Rationale**: All five in-scope endpoints already funnel through exactly
one repo method call each; wrapping there is the single smallest-diff
touch point, and keeps route/DTO/openapi code completely untouched
(FR-004: transparent to clients).

## Decision: TTL granularity

**Decision**: `RedisCache` takes `ttlMillis: Long` (not seconds), using
Redis's `PSETEX` (millisecond-precision expiry) instead of `SETEX`.

**Rationale**: Production TTL is 5 minutes (FR-003), but a unit test
proving expiry actually happens needs a short, fast-to-await TTL (tens of
milliseconds) — `SETEX`'s whole-second granularity would force a slow
real-second sleep in that test. Millisecond precision costs nothing in
production and makes the expiry test fast.

## Decision: Test infrastructure — Testcontainers for Redis AND Postgres (supersedes H2)

**Decision**: Both the Redis dependency and the existing Postgres-backed
DB tests move to Testcontainers (`org.testcontainers:testcontainers:1.21.4`
+ `org.testcontainers:postgresql:1.21.4`, Maven Central, checked
2026-07-31) — a real `postgres:16-alpine` container and a real
`redis:7-alpine` container (plain `GenericContainer`, no dedicated Redis
Testcontainers module needed — nothing beyond "start a container and
expose a port" is required), each started once per test JVM run and
shared across all test classes (mirrors the existing shared-named-H2
pattern, just with real engines instead of H2's Postgres-compatibility
mode).

**Rationale**: Explicit user direction. This is a deviation from the
Constitution's Technology Constraints section, which names "H2 for
tests" as part of the fixed stack — noted here and in `plan.md`'s
Constitution Check as a documented (not silent) deviation, per the
Governance clause's requirement that violations carry explicit
justification.

**Consequences worth naming plainly**:
- The test suite now requires Docker to be available wherever tests run
  (this dev machine has it; CI — Phase 10, not yet built — will need it
  too, e.g. via GitHub Actions' built-in Docker support).
- Test runs get slower (container startup cost) and only pass where
  Docker can run at all — a real tradeoff against H2's zero-dependency,
  always-available nature. Accepted per explicit user direction; revisit
  if this friction becomes a real problem.
- Redis state must be flushed (`FLUSHALL` against the shared test
  container) before each test that exercises caching, exactly like the
  existing "drop tables between tests" pattern for the DB.
- No auth configured on either test container (Testcontainers defaults) —
  fine for ephemeral, Docker-isolated test containers; unrelated to and
  not a precedent for the real deployed Redis/Postgres instances' auth
  posture.

**Alternatives considered**:
- **Ambient/manually-started containers** (the `my-redis`/`postgres_db`
  containers already running on this machine): zero new test dependency,
  but ties test correctness to whatever happens to be running outside the
  build — not reproducible in CI or on a fresh clone. Rejected per
  explicit user direction favoring Testcontainers' hermeticity.
- **Embedded-Redis test library**: hermetic without needing Docker, but
  Redis-only — doesn't address making the Postgres side hermetic too,
  which the user also asked for. Rejected in favor of one consistent
  Testcontainers-based approach for both.
