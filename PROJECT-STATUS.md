# PROJECT-STATUS

## Done (2026-10-30, branch feature-4/cleaning-repo)

- Schema checked against the HA recorder dump (Postgres 18, `notatki.txt`): fixed
  `statistics_meta.id` (was `data_id`), unnamed `state_attributes` table, bigint ids as `long`.
- Versions in `gradle/libs.versions.toml`; all deps bumped to latest stable (Kotlin 2.4.21,
  Ktor 3.6.1, Exposed 1.6.0, kotest 6, Testcontainers 2, Gradle 9.8.2, ...).
- NATS JetStream publisher: `statistics_short_term` rows → subject `home-assistant.statistics`
  as StatisticsService native records `{ts, dims, vals}`. REST API unchanged.
- `.devops/k8s` (Flux + kustomize + SOPS) per homelab-infra docs; Helm chart removed.
- GitHub CI: `build.yml` (check, multi-arch Jib to GHCR, image tag bump on main), `devops.yml`.
- Kotlin conventions: ktlint (`ktlint_official`) in `check`, no `!!`, shared `StatisticTable`,
  query param helpers.
- Fixed KHealth wiring: probes were 404 and `/openapi.json` had no paths (root route hidden).

## Decisions

- Pub/sub = NATS JetStream (user's broker choice), client `io.nats:jnats`. Not Kafka.
- Data = short-term statistics (5 min); StatisticsService keeps history past HA's ~10-day purge.
- Cursor = HA row id as `Nats-Msg-Id`; resume point read from the stream's last message.
  No own state, recorder DB stays read-only. Stream auto-created if missing (max age 30 days).
- Config via env vars (`DB_*`, `REDIS_*`, `NATS_*`); no `NATS_URL` = publisher off.
- Probes: `/health/live`, `/health/ready` (same as statistics-service).

## Next steps

- StatisticsService has no NATS consumer yet (its T070 Kafka channel is gated) — build a
  JetStream consumer there, or the messages only sit in the stream.
- Deploy NATS JetStream in homelab-infra (`nats.apps.svc.cluster.local:4222` expected).
- Before first deploy: age key in `.sops.yaml`, `.devops/k8s/secrets.sops.env` (`DB_PASSWORD`),
  readonly role + `pg_hba` lines, registration in homelab-infra, GHCR package public
  (`.devops/README.md`).
- API is unauthenticated and exposed at `mon-home-ass.home.arpa` (LAN only).
