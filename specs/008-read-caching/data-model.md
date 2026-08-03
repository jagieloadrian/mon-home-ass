# Data Model: Read caching layer

## Entities

### Redis cache entry (new, external, Redis-resident)

Not persisted in the app's own store — lives in the Redis instance the
app connects to, as a plain string key/value pair with Redis's native
`PSETEX` expiry.

| Field | Type | Notes |
|---|---|---|
| key | `String` | per-method prefix + serialized parameters, e.g. `"statistics:long-term:sensor.x:2026-01-01T00:00:00Z:null:null:null"` |
| value | JSON string | the cached repo result, serialized via the existing `kotlinx.serialization` DTOs (`StatisticMetaDto`, `StatisticDto`, `StatisticSummaryDto`, `EntityDto`) |
| TTL | milliseconds | Redis-native expiry set via `PSETEX`; 5 minutes (300,000ms) in production |

No relationships to any existing entity — this is purely a wrapper around
results already produced by existing repo methods; no new DTO, no new
Postgres table. `ponytail: no size/eviction cap beyond TTL — Redis itself
can be given a maxmemory/eviction policy at the infra level if this ever
matters; add app-side eviction logic only if that's not enough in
practice.`

## Cache class shape

```kotlin
class RedisCache(private val pool: JedisPooled, private val ttlMillis: Long) {
    inline fun <reified V> getOrPut(key: String, loader: () -> V?): V?
}
```

- `getOrPut` does a Redis `GET` for `key`; on a hit, deserializes the JSON
  and returns it. On a miss, calls `loader()`, and — only if the result is
  non-null (FR-005: don't cache "not found"/error outcomes) — serializes
  it and stores it via `PSETEX key ttlMillis json` before returning it.
- If `loader()` throws (e.g. DB unreachable → the existing 503 path),
  nothing is written to Redis — the exception propagates exactly as it
  does today.
- If Redis itself is unreachable, the `Jedis` calls throw — this
  surfaces as a 503 the same way a DB failure does (`StatusPages`
  already catches generic exceptions this way); there is no
  Redis-down-but-serve-from-DB fallback. `ponytail: single point of
  failure added — Redis down now breaks previously-DB-only-dependent
  reads too. Add a try/fallback-to-loader() wrap only if this is observed
  to actually happen.`

## Per-repo usage

| Repo method | Redis key prefix + params |
|---|---|
| `StatisticMetaRepo.getAll(search)` | `"statistics-meta:${search ?: ""}"` |
| `StatisticRepo.getLongTerm(statisticId, from, to, limit, offset)` | `"statistics:long-term:$statisticId:$from:$to:$limit:$offset"` |
| `StatisticRepo.getShortTerm(statisticId, from, to, limit, offset)` | `"statistics:short-term:$statisticId:$from:$to:$limit:$offset"` |
| `StatisticRepo.getSummary(statisticId, from, to)` | `"statistics:summary:$statisticId:$from:$to"` |
| `StateMetadataRepo.getAll()` | `"entities:all"` (fixed — no parameters) |

Distinct prefixes per method rule out cross-method key collisions in the
single shared Redis keyspace; `null` parameters render as the literal
string `"null"` in the key, which is fine — it's just a cache key, not
parsed back.

## Configuration

New `redis` section in `application.yaml` / `application-test.yaml`:

| Key | Production | Test |
|---|---|---|
| `redis.host` | `localhost` (or wherever Redis is deployed) | Testcontainers-assigned host |
| `redis.port` | `6379` | Testcontainers-assigned mapped port |
| `redis.ttlMillis` | `300000` (5 minutes) | short value for the app-level cache-hit tests; the TTL-expiry unit test constructs its own `RedisCache` with an even shorter TTL directly, independent of this config |
