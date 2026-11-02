# .devops

Deployment to the homelab (k3s + Flux) following the homelab platform contract
([apps.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/apps.md),
[setup.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/setup.md),
[postgres.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/postgres.md)).
Flux reads only `.devops/helm/` from the `main` branch (HelmRelease of the shared chart `charts/app` from homelab-infra,
values description: [charts/app/values.yaml](https://github.com/jagieloadrian/homelab-infra/blob/main/charts/app/values.yaml)).

| File | In git | Contents |
| --- | --- | --- |
| `helm/release.yaml` | yes, plain | HelmRelease: image, resources, HTTPRoute `mon-home-ass.home.arpa`, `values.env` → ConfigMap `mon-home-ass-config`; the chart also renders ServiceAccount, Deployment, Service, AuthorizationPolicy. `install.remediation.retries: -1`: Flux keeps retrying the first install (e.g. until the Vault secret exists), no manual `flux reconcile --force`; a failed upgrade is retried 3 times |
| Vault `secret/apps/mon-home-ass` | no | passwords → Secret `mon-home-ass-secrets` (ExternalSecret from the chart, `secrets: true` in `release.yaml`) |

## Image and deploy

The image `ghcr.io/jagieloadrian/mon-home-ass` (Jib, amd64 + arm64) is built by CI on `main`,
which commits its tag to `helm/release.yaml`. Until the first `main` build does that, the tag there is a placeholder
(`main-00000000000000-0000000`) that cannot be pulled.

- This repository is private, so GHCR creates the package as private. After the first CI build: GHCR package
  `mon-home-ass` → Package settings → visibility **Public** (the cluster pulls images without logging in).
- `main` is protected: the CI step "Deploy (bump image tag)" pushes as a GitHub App on the ruleset bypass list (variable
  `DEPLOY_APP_CLIENT_ID`, secret `DEPLOY_APP_PRIVATE_KEY`), setup in https://github.com/jagieloadrian/homelab-infra/blob/main/docs/apps.md (own app,
  step 3). The tag commit carries `[skip ci]`.

## values.env (`helm/release.yaml`)

| Variable | Value | Meaning |
| --- | --- | --- |
| `DB_TYPE` / `DB_DRIVER` | `postgres` / `org.postgresql.Driver` | HA recorder engine |
| `DB_URL` | `postgres.apps.svc.cluster.local:5432` | `host:port` of Postgres on Wyse (Service from homelab-infra) |
| `DB_NAME` | `home_assistant` | HA recorder database |
| `DB_USER` | `mon_home_ass` | read-only role (below) |
| `CORS_HOSTS` | `""` | comma-separated hosts without scheme allowed to call the API from a browser on another origin (app default `localhost:8080`); empty = none, the API is used same-origin at `mon-home-ass.home.arpa` |

Redis is not required: without it the cache lives in pod memory (`REDIS_HOST`/`REDIS_PORT`, once there is one).

`/health/ready` answers 503 while the database is unreachable, so the pod only leaves the Service; `/health/live`
(startup and liveness probes) stays 200 and the pod is not restarted.

## NATS (not set up yet)

The integration with the homelab NATS is not set up yet: NATS has no user or policy for mon-home-ass, so
`values.env` has no `NATS_*` variables. Without `NATS_URL` the statistics publisher stays off and the API works as usual.
Wiring it needs:

1. A NATS user for mon-home-ass with its permissions in homelab-infra
   [apps/nats/release.yaml](https://github.com/jagieloadrian/homelab-infra/blob/main/apps/nats/release.yaml):
   publish to its subject and `$JS.API.>` (stream lookup and creation, last published message), subscribe `_INBOX.>`
   (replies).
2. Principal `cluster.local/ns/apps/sa/mon-home-ass` in the AuthorizationPolicy `nats` (same file).
3. The credentials in Vault `secret/apps/mon-home-ass`. The app reads them only from the URL, so `NATS_URL` goes there,
   not into `values.env`: `vault kv patch secret/apps/mon-home-ass NATS_URL=-` with
   `nats://<user>:<password>@nats.apps.svc.cluster.local:4222`.
4. To feed statistics-service, a subject under `stats.ingest.>` (`NATS_SUBJECT=stats.ingest.<source id>`) with
   `NATS_STREAM=STATS_INGEST`, the stream statistics-service already owns (JetStream refuses a second stream over the
   same subjects), plus a `pubsub` source there with a mapping for the published JSON. That source expects an
   `Authorization: Bearer <source token>` header, which the publisher does not send yet.

The other variables go into `values.env` (defaults in `application.yaml`): `NATS_STREAM` (`HOME_ASSISTANT`, created
if missing, max age 30 days), `NATS_SUBJECT` (`home-assistant.statistics`) and `NATS_INTERVAL_SECONDS` (`60`, how
often to check for new `statistics_short_term` rows).

## Secrets (Vault)

Secrets live only in Vault `secret/apps/mon-home-ass` (keys = env names); nothing in git, CI needs no keys.
Set them per [vault.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/vault.md):

```bash
vault kv put secret/apps/mon-home-ass DB_PASSWORD=-
```

Type the password from `openssl rand -hex 24`, then press Ctrl+D twice (no Enter, it would become part of the
value). The same password goes into the role below.

## Postgres: read-only role

On Wyse (`ssh wyse sudo -u postgres psql -d home_assistant`):

```sql
CREATE ROLE mon_home_ass LOGIN PASSWORD '<DB_PASSWORD>';
ALTER ROLE mon_home_ass SET default_transaction_read_only = on;
GRANT CONNECT ON DATABASE home_assistant TO mon_home_ass;
GRANT USAGE ON SCHEMA public TO mon_home_ass;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mon_home_ass;
ALTER DEFAULT PRIVILEGES FOR ROLE <HA recorder table owner> IN SCHEMA public GRANT SELECT ON TABLES TO mon_home_ass;
```

`<HA recorder table owner>` is the role Home Assistant writes the recorder with: `\dt` in the same psql session lists
the tables with their `Owner`. The default privileges make tables that HA creates later (migrations) readable too.

`pg_hba.conf` currently allows only the `statistics` database from the cluster. Add (then `SELECT pg_reload_conf();`):

```
host  home_assistant  mon_home_ass  10.42.0.0/16    scram-sha-256
host  home_assistant  mon_home_ass  <RPi5 IP>/32    scram-sha-256
```

## Registration in homelab-infra

Already registered in homelab-infra
[clusters/home/apps/mon-home-ass.yaml](https://github.com/jagieloadrian/homelab-infra/blob/main/clusters/home/apps/mon-home-ass.yaml):
a GitRepository with `secretRef` (this repository is private) and a Kustomization of `.devops/helm/` into namespace
`apps`. Change it in `<homelab-infra checkout>/clusters/home/apps/mon-home-ass.yaml`; do not regenerate it from
`_app.yaml.example`, that would overwrite the file and drop the `secretRef`.
