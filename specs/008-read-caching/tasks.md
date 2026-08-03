---

description: "Task list template for feature implementation"
---

# Tasks: Read caching layer

**Input**: Design documents from `/specs/008-read-caching/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included — Constitution Principle V requires new utility logic and changed repo behavior ship with a test.

**Organization**: Single user story (spec.md has only P1) — the entire feature is
one story, so Foundational delivers the whole implementation (including
the test-infrastructure rewrite, since nothing downstream is testable
without it) and the User Story phase is the caching-specific tests.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Add `redis.clients:jedis:7.5.3` (implementation) and `org.testcontainers:testcontainers:1.21.4` + `org.testcontainers:postgresql:1.21.4` (testImplementation) to `build.gradle.kts`
- [X] T002 Add a `redis` section to `src/main/resources/application.yaml` (`host: "localhost"`, `port: 6379`, `ttlMillis: 300000`) — matches the already-running local Redis container

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Test infrastructure rewrite (Testcontainers) plus the cache utility and every repo wrap — nothing in Phase 3 is testable until this lands.

- [X] T003 Rewrite `src/test/kotlin/com/anjo/monhomeass/testutils/TestDatabase.kt`: add a lazily-started, JVM-shared `PostgreSQLContainer` (`postgres:16-alpine`, matches production engine) and a lazily-started, JVM-shared `GenericContainer` (`redis:7-alpine`, exposed port 6379) as singletons; `prepareDatabase()` now calls `Database.connect(...)` against the Postgres container's live `jdbcUrl`/`username`/`password` instead of the H2 URL; add `testApplicationConfig(): MapApplicationConfig` building the `ktor.database.*` and `ktor.redis.*` keys from both containers' live host/port (replaces every test's `ApplicationConfig("application-test.yaml")`); add `flushRedis()` calling `FLUSHALL` against the Redis container, for use in each test's setup alongside the existing `dropTableIfExists()` pattern (depends on T001)
- [X] T004 [P] Update all 5 existing route test files (`StatisticsMetaRoutesKtTest.kt`, `StatisticLongTermRoutesKtTest.kt`, `StatisticShortTermRoutesKtTest.kt`, `StatisticSummaryRoutesKtTest.kt`, `EntitiesRoutesKtTest.kt`) to call `environment { config = testApplicationConfig() }` instead of `ApplicationConfig("application-test.yaml")`, and call `flushRedis()` in each test's setup function — mechanical change only, no assertion changes (depends on T003)
- [X] T005 [P] Create `RedisCache` in `src/main/kotlin/com/anjo/monhomeass/cache/RedisCache.kt` — constructor takes `pool: JedisPooled, ttlMillis: Long`; `inline fun <reified V> getOrPut(key: String, loader: () -> V?): V?` does a Redis `GET`, deserializes via `kotlinx.serialization.json.Json` on a hit; on a miss calls `loader()` and, only if non-null (FR-005), serializes and stores via `PSETEX key ttlMillis json`; add the `// ponytail:` comment from data-model.md noting no eviction cap and no Redis-down fallback (depends on T001)
- [X] T006 Provide `JedisPooled` and `RedisCache` via DI in `src/main/kotlin/com/anjo/monhomeass/plugins/DependencyInjection.kt`, reading `redis.host`/`redis.port`/`redis.ttlMillis` from `environment.config` the same way `configureDatabases()` reads its own config section (depends on T005)
- [X] T007 Change `StatisticMetaRepo`/`StatisticRepo`/`StateMetadataRepo` constructors to take `RedisCache` as a parameter (so DI injects it), and wrap each method's existing query per the key scheme in data-model.md: `StatisticMetaRepo.getAll` → `"statistics-meta:${search ?: ""}"`; `StatisticRepo.getLongTerm`/`getShortTerm` → `"statistics:long-term:..."`/`"statistics:short-term:..."` with all 5 params; `StatisticRepo.getSummary` → `"statistics:summary:..."` with 3 params; `StateMetadataRepo.getAll` → fixed `"entities:all"` (depends on T006)

**Checkpoint**: All 5 read endpoints serve from Redis on repeat calls within 5 minutes; full existing test suite still passes on the new Testcontainers-backed infrastructure.

---

## Phase 3: User Story 1 - Repeated reads skip the database round-trip (Priority: P1) 🎯 MVP

**Goal**: A repeated request within the 5-minute window is served from cache (no fresh DB query); different requests stay isolated; expiry and null-skip behavior are correct.

**Independent Test**: Call the same cached endpoint twice; mutate the underlying row directly in the DB between calls; the second call still returns the pre-mutation value within the TTL.

- [X] T008 [US1] Add `RedisCacheTest.kt` in `src/test/kotlin/com/anjo/monhomeass/cache/` — unit test on `RedisCache` directly against the Testcontainers Redis instance (flush before each test): repeat `getOrPut` with the same key returns the cached value without invoking `loader` again; different keys are isolated; a short constructed TTL (e.g. 50ms) expires and the next `getOrPut` call invokes `loader` again; a `null` result from `loader` is never cached (next call invokes `loader` again immediately)
- [X] T009 [P] [US1] Add a cache-hit test case to `StatisticsMetaRoutesKtTest.kt`: call `GET /statistics-meta`, mutate the seeded row directly via `StatisticMetaData.update`, call again — assert the pre-mutation value is still returned
- [X] T010 [P] [US1] Add the equivalent cache-hit test case to `EntitiesRoutesKtTest.kt` for `GET /entities`
- [X] T011 [P] [US1] Add the equivalent cache-hit test case to `StatisticLongTermRoutesKtTest.kt` for `GET /statistics/{statisticId}`
- [X] T012 [P] [US1] Add the equivalent cache-hit test case to `StatisticShortTermRoutesKtTest.kt` for the `/short-term` variant
- [X] T013 [P] [US1] Add the equivalent cache-hit test case to `StatisticSummaryRoutesKtTest.kt` for `GET /statistics/{statisticId}/summary`

**Checkpoint**: Feature fully functional and testable — this is the whole feature.

---

## Phase 4: Polish & Cross-Cutting Concerns

- [X] T014 Run `./gradlew build` — full build + test suite passes (Constitution Principle IV; requires Docker running locally for Testcontainers) (depends on T001–T013)
- [X] T015 Run `quickstart.md` steps against a running instance to confirm end-to-end behavior, including `redis-cli KEYS '*'` verification

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: T003–T007 depend on T001/T002 as noted. Delivers the whole feature plus the test-infra migration (single user story, but Phase 3 can't run without this).
- **User Story 1 (Phase 3)**: Depends on Phase 2.
- **Polish (Phase 4)**: Depends on Phase 3 complete.

### Parallel Opportunities

- T004 (test-file config migration) and T005 (`RedisCache` class) touch disjoint files — [P] once T003/T001 land respectively.
- T009–T013 are five different test files — all [P] once T008's shared utility exists (T008 doesn't block them, it only tests `RedisCache` in isolation).

---

## Parallel Example: Phase 2

```bash
# T004 and T005 touch disjoint files — safe together.
Task: "Migrate all 5 route test files to testApplicationConfig()"
Task: "Create RedisCache utility class"
```

---

## Implementation Strategy

### MVP First (and only)

1. Complete Phase 1: Setup (dependencies, config)
2. Complete Phase 2: Foundational (test infra rewrite + cache utility + repo wraps — verify the existing test suite still passes on the new infra before adding new tests)
3. Complete Phase 3: User Story 1 (new cache-behavior tests)
4. **STOP and VALIDATE**: repeat a call after mutating the DB directly, confirm the stale cached value is returned within 5 minutes; `redis-cli KEYS '*'` shows real entries
5. Complete Phase 4: Polish (build, quickstart)
6. Ship — this is the entire feature, no incremental stories to add later
