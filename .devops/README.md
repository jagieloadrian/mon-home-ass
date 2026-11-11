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

- The GHCR package `mon-home-ass` can stay private: the cluster pulls with Secret `ghcr-pull`, a read-only GHCR token
  from Vault `secret/apps/ghcr` (homelab-infra
  [setup.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/setup.md), step 3).
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
| `PUBLISHER` | `jetstream` | send new Home Assistant statistics to statistics-service over the homelab NATS |
| `SOURCE_ID` | `home-assistant` | the statistics-service source; the subject is `stats.ingest.home-assistant` |
| `NATS_URL` | `nats://nats.apps.svc.cluster.local:4222` | homelab NATS |
| `NATS_USER` | `mon-home-ass` | NATS user from homelab-infra; its password is `NATS_PASSWORD` in Vault |
| `NATS_INBOX_PREFIX` | `_INBOX_mon_home_ass` | the only inbox the user may subscribe to; JetStream acks arrive there |

Redis is not required: without it the cache lives in pod memory (`REDIS_HOST`/`REDIS_PORT`, once there is one).

`/health/ready` answers 503 while the database is unreachable, so the pod only leaves the Service; `/health/live`
(startup and liveness probes) stays 200 and the pod is not restarted.

## Statistics publisher (NATS)

Every 60 s the app sends new `statistics_short_term` rows (Home Assistant's 5-minute statistics) to statistics-service
over NATS JetStream. How it works and all variables: [README](../README.md#statistics-publisher).

homelab-infra already has the NATS side: user `mon-home-ass` may publish to `stats.ingest.home-assistant` only and
receive acks on `_INBOX_mon_home_ass.>`, and the AuthorizationPolicy `nats` lets this app's ServiceAccount reach
port 4222. Without `SOURCE_TOKEN` the publisher logs an error and stays off; the API works either way.

Once, after statistics-service runs. The gateway serves plain HTTP, so the URLs use `http://`. `NATS_PASSWORD`
is already in Vault: homelab-infra `scripts/install.sh` copies `MON_HOME_ASS_PASSWORD` from `secret/apps/nats`.

1. Sign in as the statistics-service admin (`ADMIN_EMAIL`, password from `scripts/install.sh`):
   ```bash
   API=http://statistics.home.arpa/api/v2
   read -rsp "Admin password: " pw && echo
   TOKEN=$(jq -n --arg p "$pw" '{email:"admin@home.arpa",password:$p}' | curl -s $API/auth/login -H 'Content-Type: application/json' -d @- | jq -r .token); unset pw
   ```
2. Import [`definitions/home-assistant.json`](https://github.com/jagieloadrian/statistics-service/blob/main/definitions/home-assistant.json)
   (dataset, `pubsub` source and dashboard):
   ```bash
   curl -sfL https://raw.githubusercontent.com/jagieloadrian/statistics-service/main/definitions/home-assistant.json |
     curl -s -X POST $API/import -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d @-
   ```
3. Create the source token and put it straight into Vault. The token is shown only once, so it never reaches the
   screen; a second run creates a new one:
   ```bash
   t=$(curl -s -X POST $API/sources/home-assistant/credential -H "Authorization: Bearer $TOKEN" | jq -r .token) &&
     [ "$t" != null ] && printf %s "$t" | kubectl -n vault exec -i vault-0 -- vault kv patch secret/apps/mon-home-ass SOURCE_TOKEN=- >/dev/null && echo OK; unset t
   ```
   Vault in the pod must be logged in ([vault.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/vault.md)).
4. Sync the Secret, wait for it, then restart the app:
   ```bash
   kubectl -n apps annotate externalsecret mon-home-ass-secrets force-sync=$(date +%s) --overwrite
   sleep 10 && kubectl -n apps rollout restart deploy/mon-home-ass
   ```
5. Check: the log shows `Published N statistics to jetstream` within a minute, and
   `curl -s $API/sources/home-assistant/status -H "Authorization: Bearer $TOKEN"` shows accepted records and no
   rejections.

## Secrets (Vault)

Secrets live only in Vault `secret/apps/mon-home-ass` (keys = env names); nothing in git, CI needs no keys.
Set them per [vault.md](https://github.com/jagieloadrian/homelab-infra/blob/main/docs/vault.md):

```bash
vault kv put secret/apps/mon-home-ass DB_PASSWORD=-
```

| Key | Value |
| --- | --- |
| `DB_PASSWORD` | password of the read-only role below |
| `NATS_PASSWORD` | `MON_HOME_ASS_PASSWORD` from `secret/apps/nats` |
| `SOURCE_TOKEN` | token of the statistics-service source `home-assistant` (publisher section above) |

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
