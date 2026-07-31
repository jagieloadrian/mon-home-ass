# Quickstart: Validating the Dependency Update

Prerequisites: Docker running (Testcontainers-backed Redis/Postgres tests
need it), JDK 17 available (this environment's `java-21` install lacks
`javac`; use `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`).

## 1. Confirm the bump landed

```sh
grep -E "kotlin_version|ktor_version|exposed_version|postgres_version|h2_version|logback_version" gradle.properties
```

Expected values after this feature: see `research.md` table (Kotlin 2.4.0,
Ktor 3.5.1, Exposed 1.0.0 — unchanged, PostgreSQL JDBC 42.7.13, H2 2.4.240,
Logback 1.6.1), or whatever is current stable at implementation time if
newer releases shipped since this plan was written (per FR-001, always
re-check against current upstream, don't blindly copy these numbers).

## 2. Full build + test suite (this is the acceptance gate — FR-002/SC-002)

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build
```

Expected outcome: `BUILD SUCCESSFUL`, zero failing/skipped tests, same test
count as before the bump (no tests removed to route around a break).

## 3. Confirm no observable API behavior change (FR-005/SC-004)

Start the app and hit a couple of existing endpoints, comparing shape
against `openapi.yaml` / the live `/swagger` doc — same as any other change
per the project's existing manual-check convention (see root `CLAUDE.md`):

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew run
curl -s localhost:8080/statistics-meta | head
curl -s localhost:8080/swagger
```

Expected: identical response shapes/status codes to pre-bump behavior.

## 4. Confirm single-PR delivery (FR-004/SC-003)

```sh
git log --oneline main..HEAD
```

Expected: this feature's dependency changes are part of one PR (may be one
or a few commits, but merged as a single unit — not one PR per dependency).
