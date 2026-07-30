# Research: Statistic Entities List

No `NEEDS CLARIFICATION` markers remain in Technical Context — stack, storage,
and testing approach are all fixed by the project constitution and the
existing codebase pattern (recorder/migration/schema-changes/events
endpoints already establish table→DTO→repo→route). One genuinely open
decision remained: how to satisfy FR-006 (503 on DB/query failure), since no
route in this codebase currently handles that case.

## Decision: DB-failure → 503 mapping

**Decision**: Install Ktor's `StatusPages` plugin (`io.ktor:ktor-server-status-pages-jvm`,
resolved via the existing `io.ktor.plugin` gradle plugin's BOM — no explicit
version needed) in a new `plugins/StatusPages.kt`, and register an exception
handler for Exposed/JDBC failures (`ExposedSQLException`, `SQLException`)
that responds `503 Service Unavailable` with a small JSON error body.

**Rationale**: This is the standard, native Ktor mechanism for centralized
error-to-status mapping — a single install call, not a hand-rolled
try/catch abstraction. Handling it once in `StatusPages` (rather than per
route) also means Phase 2-5 statistics endpoints (same roadmap) get the
503 behavior for free once they exist, instead of duplicating a catch block
in every route handler.

**Alternatives considered**:
- Per-route `try/catch` in `StatisticsRoutes.kt` — rejected: duplicates the
  same mapping logic in every future statistics route (Phases 2-5), and
  the constitution's own bug-fix guidance favors fixing shared behavior in
  one place all callers route through.
- Custom exception + Ktor `install(CallLogging)` combo — rejected: more
  moving parts than `StatusPages` for the same outcome; `StatusPages` is
  already the idiomatic Ktor answer to "map exception type → status code".

## Everything else: no research needed

- **DTO/repo/route shape** — copy `RecorderDto`/`RecorderRepo`/`recorderRout`
  pattern (`src/main/kotlin/com/anjo/monhomeass/model/dto/RecorderDto.kt`,
  `repository/RecorderRepo.kt`, `routing/RecorderRoutes.kt`) verbatim in
  structure, swapping in `StatisticMetaData` columns.
- **OpenAPI/swagger** — already wired globally in `Routing.kt`
  (`swaggerUI` + `OpenApiDocSource.Routing`); new route just needs its own
  `.describe { }` block, same as every existing route.
- **Test harness** — `TestDatabase.kt` + `testApplication { }` pattern
  already used by `RecorderRoutesKtTest`/`SchemaChangesRoutesKtTest`, reused
  as-is.
