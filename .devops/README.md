# .devops

Deployment to the homelab (k3s + Flux) following the contract from `homelab-infra/docs`
([setup.md](../../homelab-infra/docs/setup.md), [postgres.md](../../homelab-infra/docs/postgres.md)).
Flux reads only `.devops/helm/` from the `main` branch (HelmRelease of the shared chart `charts/app` from homelab-infra,
values description: `homelab-infra/charts/app/values.yaml`).

| File | In git | Contents |
| --- | --- | --- |
| `helm/release.yaml` | yes, plain | HelmRelease: image, resources, HTTPRoute `mon-home-ass.home.arpa`, `values.env` → ConfigMap `mon-home-ass-config`; the chart also renders ServiceAccount, Deployment, Service, AuthorizationPolicy |
| Vault `secret/apps/mon-home-ass` | no | passwords → Secret `mon-home-ass-secrets` (ExternalSecret from the chart, `secrets: true` in `release.yaml`) |

The image `ghcr.io/jagieloadrian/mon-home-ass` (Jib, amd64 + arm64) is built by CI on `main`,
which commits its tag to `helm/release.yaml`.

## values.env (`helm/release.yaml`)

| Variable | Value | Meaning |
| --- | --- | --- |
| `DB_TYPE` / `DB_DRIVER` | `postgres` / `org.postgresql.Driver` | HA recorder engine |
| `DB_URL` | `postgres.apps.svc.cluster.local:5432` | `host:port` of Postgres on Wyse (Service from homelab-infra) |
| `DB_NAME` | `home_assistant` | HA recorder database |
| `DB_USER` | `mon_home_ass` | read-only role (below) |
| `NATS_URL` | `nats://nats.apps.svc.cluster.local:4222` | JetStream broker; variable unset = publisher disabled, API keeps working |
| `NATS_STREAM` / `NATS_SUBJECT` | `HOME_ASSISTANT` / `home-assistant.statistics` | stream created by the application if it does not exist (max age 30 days) |
| `NATS_INTERVAL_SECONDS` | `60` | how often to check for new `statistics_short_term` rows |

Redis is not required: without it the cache lives in pod memory (`REDIS_HOST`/`REDIS_PORT`, once there is one).

## Secrets (Vault)

Secrets live only in Vault `secret/apps/mon-home-ass` (keys = env names); nothing in git, CI needs no keys.
Set them per [vault.md](../../homelab-infra/docs/vault.md):

```bash
vault kv put secret/apps/mon-home-ass DB_PASSWORD=-   # value from stdin (openssl rand -hex 24)
```

NATS broker with authentication: remove `NATS_URL` from `values.env` and put it in Vault
(`vault kv patch secret/apps/mon-home-ass NATS_URL=-`) as `nats://<user>:<password>@nats.apps.svc.cluster.local:4222`.

## Postgres: read-only role

On Wyse (`ssh wyse sudo -u postgres psql -d home_assistant`):

```sql
CREATE ROLE mon_home_ass LOGIN PASSWORD '<DB_PASSWORD>';
ALTER ROLE mon_home_ass SET default_transaction_read_only = on;
GRANT CONNECT ON DATABASE home_assistant TO mon_home_ass;
GRANT USAGE ON SCHEMA public TO mon_home_ass;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mon_home_ass;
-- tables HA will create in future migrations
ALTER DEFAULT PRIVILEGES FOR ROLE home_ass_user IN SCHEMA public GRANT SELECT ON TABLES TO mon_home_ass;
```

`pg_hba.conf` currently allows only the `statistics` database from the cluster. Add (then `SELECT pg_reload_conf();`):

```
host  home_assistant  mon_home_ass  10.42.0.0/16    scram-sha-256
host  home_assistant  mon_home_ass  <RPi5 IP>/32    scram-sha-256
```

## Registration in homelab-infra

```bash
cd ~/IdeaProjects/homelab-infra/clusters/home/apps
cp _app.yaml.example mon-home-ass.yaml && sed -i 's/APP/mon-home-ass/g' mon-home-ass.yaml
git add mon-home-ass.yaml && git commit -m 'feat(apps): mon-home-ass' && git push
```

GHCR package `mon-home-ass` → Package settings → visibility **Public** (the cluster pulls images without logging in).

NATS broker with AuthorizationPolicy: allow principal `cluster.local/ns/apps/sa/mon-home-ass`.
