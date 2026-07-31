<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/008-read-caching/plan.md
<!-- SPECKIT END -->

# mon-home-ass

Kotlin/Ktor API exposing Home Assistant recorder DB stats (readonly DB user).
Stack: Ktor + Exposed + Postgres (H2 for tests), kotlinx.serialization,
ktor-server-routing-openapi/swaggerUI.

Full rules: `.specify/memory/constitution.md`. Key points every session must
follow:
- Readonly DB only — never write/update/delete against the recorder DB.
- YAGNI: reuse the table → DTO → repo → route pattern already in the repo;
  no pagination/caching/search until a real need shows up
  (see `user-roadmap.md`).
- Run under `ponytail` + `caveman` for the whole task, not just when the
  user says the words.
- After every `speckit-implement`, the app must build and start
  (`./gradlew build`) — a broken build is not "done".
- New repos/routes/non-trivial logic ship with a test (see `src/test/kotlin`
  for the existing pattern).

Design contract: `openapi.yaml` (API shape) + `user-roadmap.md` (phase plan).
