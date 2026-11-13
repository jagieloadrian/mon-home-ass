# PROJECT-STATUS

## Done (2026-11-13, branch feature-12/sensor-catalog)

- Sensor catalog `GET /sensors?area=&category=`: name, area, floor, category, unit per `statistics_meta` row.
  - Without `HA_TOKEN`: from the recorder DB (`friendly_name`, `device_class`, unit of the latest state
    attributes; `statistics_meta` name/unit/unit_class).
  - With `HA_URL` + `HA_TOKEN`: plus Home Assistant registries over the WebSocket API (entity/device/area/floor):
    user-given names, area (entity or its device), floor, `entity_category` diagnostic/config -> `diagnostic`.
    HA unreachable or token rejected -> DB-only catalog, warning in the log.
  - Category from `device_class` (climate, air-quality, energy, utility, battery, light, diagnostic), fallback
    `unit_class`, else `other`. Duplicate names get ` 2`, ` 3`.
- Publisher dims: `statistic_id` plus `name`, `area`, `floor`, `category`, `unit`; everything is published.
- Recorder internals moved under `/raw`: `/raw/migrations`, `/raw/schemas`, `/raw/recorders`,
  `/raw/statistics-runs`, `/raw/events`.
- `.devops/scripts/ha-token-to-vault.sh` stores the HA long-lived token as `HA_TOKEN` in Vault; `HA_URL` in
  `release.yaml` points at homelab-infra Service `home-assistant.apps:8123` (homelab-infra branch
  `feature/home-assistant-service`).

## Done (2026-11-02, branch feature-5/remove-comments)

- No comments in code/config; tests keep `// given` / `// when` / `// then` markers.
- Deploy: HelmRelease retries the first install until Vault has `secret/apps/mon-home-ass`; NATS settings removed until a NATS user and policy exist in homelab-infra.
- CORS from `CORS_HOSTS` (comma-separated hosts, empty = no cross-origin) instead of any host.
- `/health/ready` returns 503 while the database is unreachable; `/health/live` does not depend on the database.
- Logging at `info` by default.

## Done (2026-11-02, branch feature-4/cleaning-repo)

- Deploy moved from `.devops/k8s` (kustomize) to `.devops/helm`: Flux HelmRelease of homelab-infra
  `charts/app`, config in `values.env`, secrets in Vault `secret/apps/mon-home-ass` (ExternalSecret, `secrets: true`);
  CI bumps the tag in `release.yaml`, `devops.yml` checks with `helm template` + kubeconform.

## Done (2026-10-30, branch feature-4/cleaning-repo)

- Schema checked against the HA recorder dump (Postgres 18, `notatki.txt`): fixed
  `statistics_meta.id` (was `data_id`), unnamed `state_attributes` table, bigint ids as `long`.
- Versions in `gradle/libs.versions.toml`; all deps bumped to latest stable (Kotlin 2.4.21,
  Ktor 3.6.1, Exposed 1.6.0, kotest 6, Testcontainers 2, Gradle 9.8.2, ...).
- NATS JetStream publisher: `statistics_short_term` rows → subject `home-assistant.statistics`
  as StatisticsService native records `{ts, dims, vals}`. REST API unchanged.
- `.devops/k8s` (Flux + kustomize + SOPS) per homelab-infra docs, now `.devops/helm` (above); own Helm chart removed.
- GitHub CI: `build.yml` (check, multi-arch Jib to GHCR, image tag bump on main), `devops.yml`.
- Kotlin conventions: ktlint (`ktlint_official`) in `check`, no `!!`, shared `StatisticTable`,
  query param helpers.
- Fixed KHealth wiring: probes were 404 and `/openapi.json` had no paths (root route hidden).

## Decisions

- Sensor metadata from the HA WebSocket registries, not from `.storage` files (undocumented, would pin the pod to Wyse).
- HA token belongs to a separate non-admin HA user; the token is pasted into the script, the script does not log in
  to HA (least privilege). HA has no read-only users: a leaked token can call services.

- Pub/sub = NATS JetStream (user's broker choice), client `io.nats:jnats`. Not Kafka.
- Data = short-term statistics (5 min); StatisticsService keeps history past HA's ~10-day purge.
- Cursor = HA row id as `Nats-Msg-Id`; resume point read from the stream's last message.
  No own state, recorder DB stays read-only. Stream auto-created if missing (max age 30 days).
- Config via env vars (`DB_*`, `REDIS_*`, `NATS_*`); no `NATS_URL` = publisher off.
- Probes: `/health/live`, `/health/ready` (same as statistics-service).

## Next steps

- statistics-service `definitions/home-assistant.json`: dimensions `name`, `area`, `floor`, `category`, `unit`
  and dashboard variables area/category (stage 4 of the proposal).
- Stage 3: per-room summary (`/areas/{area}`) and energy consumption per hour/day from cumulative `sum`.
- Not checked against a real Home Assistant: registry message fields, `config/floor_registry/list` needs HA 2024.4+
  (older HA: whole registry fetch fails, DB-only catalog), whether a non-admin token may list the registries.
- Latest-attributes query scans `states` once per cache TTL (5 min); watch it on a large recorder DB.

- StatisticsService has no NATS consumer yet (its T070 Kafka channel is gated) — build a
  JetStream consumer there, or the messages only sit in the stream.
- Deploy NATS JetStream in homelab-infra (`nats.apps.svc.cluster.local:4222` expected).
- Before first deploy: `DB_PASSWORD` in Vault `secret/apps/mon-home-ass` (homelab-infra docs/vault.md),
  readonly role + `pg_hba` lines, registration in homelab-infra, GHCR package public
  (`.devops/README.md`).
- API is unauthenticated and exposed at `mon-home-ass.home.arpa` (LAN only).
