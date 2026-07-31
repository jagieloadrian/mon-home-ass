# Research: Dependency Updates

## Current stable upstream versions (checked 2026-07-31)

| Dependency | Pinned (gradle.properties) | Latest stable | Bump type |
|---|---|---|---|
| Kotlin | 2.3.0 | 2.4.0 | minor |
| Ktor | 3.4.0 | 3.5.1 | minor |
| Exposed | 1.0.0 | 1.3.1 | minor |
| PostgreSQL JDBC | 42.7.2 | 42.7.13 | patch |
| H2 | 2.2.224 | 2.4.240 | minor |
| Logback | 1.5.13 | 1.6.1 | minor |

No dependency currently has a newer *major* version available (Postgres JDBC
is still on the 42.x line, no v43 exists yet; Kotlin/Ktor/Logback moves are
all minor-version bumps within their current major). The Clarification to
always take the latest major even if substantial rework is required doesn't
change the diff this round — it just means a future run of this same
maintenance pass should not cap itself at the current major line if one
becomes available.

## Kotlin 2.3.0 → 2.4.0

- Decision: bump `kotlin_version` to 2.4.0.
- Rationale: latest stable; K1 compiler support removed in 2.4.0 but this
  project already builds under K2 (Kotlin 2.x default), so no action needed.
- Breaking changes checked: nullable Java type-argument flexibility no
  longer applied (stricter null-safety errors possible at call sites);
  standalone `org.jetbrains.kotlin.js` plugin removed (project doesn't use
  Kotlin/JS). Netty/JVM-only codebase — main exposure is the nullable
  Java-interop check, which surfaces at compile time if it applies.
- Alternatives considered: staying on 2.3.x — rejected, contradicts
  Clarification (always take latest) and there's no reason to defer a minor,
  non-breaking-for-this-codebase bump.

## Ktor 3.4.0 → 3.5.1

- Decision: bump `ktor_version` to 3.5.1.
- Rationale: latest stable; the `build.gradle.kts` comment
  (`// will be removed with ktor 3.4.1`) on `ktor-server-routing-openapi`
  refers to an unintentional hard dependency introduced in 3.4.0 that gets
  corrected in a later patch — bumping past that point is itself the fix,
  so this dependency and comment should be re-checked/removed once on 3.5.1.
- Breaking changes checked: none affecting this codebase's usage (routing,
  content negotiation, CORS, default headers, status pages, DI, config-yaml,
  swagger, routing-openapi) found in the 3.4→3.5 changelog beyond the
  routing-openapi coupling fix above.
- Alternatives considered: none — single newest stable line available.

## Exposed 1.0.0 → 1.3.1

- Decision: bump `exposed_version` to 1.3.1.
- Rationale: latest stable (released 2026-07-01, after this plan's initial
  research pass which had only found 1.0.0 from January); 1.0.0 established
  a stable API with no breaking changes guaranteed until the next major
  release, so 1.x → 1.3.1 is a safe in-line bump.
- Breaking changes checked: none — covered by Exposed's own 1.0 stability
  guarantee; confirmed by the full test suite (61/61 passing unchanged).

## PostgreSQL JDBC 42.7.2 → 42.7.13

- Decision: bump `postgres_version` to 42.7.13.
- Rationale: latest patch release on the only currently-available major
  line (42.x); pure bugfix/connection-property additions, no known breaking
  change for this project's usage (Exposed + `DriverManager`-style
  connection via readonly Postgres).

## H2 2.2.224 → 2.4.240

- Decision: bump `h2_version` to 2.4.240.
- Rationale: latest stable; used only as the test-time in-memory DB (never
  touches the real, readonly Postgres recorder DB, so no production-data
  risk from schema/type mapping changes).
- Breaking changes checked: `datetime()` column type now maps to
  `TIMESTAMP(9)` instead of `DATETIME(9)` in Regular/MySQL/MariaDB modes;
  `ubyte()` maps to `NUMBER(3)` instead of `NUMBER(4)` in Oracle/H2-Oracle
  modes. Exposed's own docs note this is handled fine by its built-in schema
  creation and shouldn't need app-level changes, but the full test suite run
  (FR-002) is what actually confirms this for this project's specific table
  definitions (`epochSeconds`, numeric columns, etc.) rather than assuming
  it from the changelog alone.

## Logback 1.5.13 → 1.6.1

- Decision: bump `logback_version` to 1.6.1.
- Rationale: latest stable; this project only uses `logback-classic` as a
  runtime SLF4J backend via `application.yaml`/`logback.xml`-style config
  (via `kotlin-logging-jvm`), no direct calls to the Logback Java API in
  source.
- Breaking changes checked: 1.6.0 removed already-deprecated
  variables/methods/classes, and `AsyncAppenderBase.put(ILoggingEvent)`
  visibility changed to `protected`. Neither is used directly by this
  project's code (no custom appenders), so no source changes expected —
  confirmed by the test suite rather than assumed.

## Out of scope (confirmed against spec Assumptions)

`jedis` (7.5.3, hardcoded in `build.gradle.kts`), `khealth` (3.0.0),
`kotlin-logging-jvm` (7.0.14), `kotest-assertions-core` (5.9.1), `mockk`
(1.13.13), and `testcontainers`/`testcontainers-postgresql` (1.21.4) are not
listed in `gradle.properties` and are therefore out of scope per the spec's
Assumptions section — this pass touches only the six `gradle.properties`
version properties.
