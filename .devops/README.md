# .devops

Wdrożenie na homelab (k3s + Flux) według kontraktu z `homelab-infra/docs`
([setup.md](../../homelab-infra/docs/setup.md), [postgres.md](../../homelab-infra/docs/postgres.md)).
Flux czyta tylko `.devops/k8s/` z gałęzi `main`.

| Plik | W gicie | Zawartość |
| --- | --- | --- |
| `k8s/mon-home-ass.yaml` | tak | ServiceAccount, Deployment, Service, HTTPRoute `mon-home-ass.home.arpa`, AuthorizationPolicy |
| `k8s/config.env` | tak, jawny | konfiguracja → ConfigMap `mon-home-ass-config` |
| `k8s/secrets.sops.env` | tak, zaszyfrowany | hasła → Secret `mon-home-ass-secrets` |
| `k8s/kustomization.yaml` | tak | generatory ConfigMap/Secret (hash w nazwie = rollout po zmianie) |

Obraz `ghcr.io/jagieloadrian/mon-home-ass` (Jib, amd64 + arm64) buduje CI na `main`
i commituje jego tag do `k8s/mon-home-ass.yaml`.

## config.env

| Zmienna | Wartość | Znaczenie |
| --- | --- | --- |
| `DB_TYPE` / `DB_DRIVER` | `postgres` / `org.postgresql.Driver` | silnik recordera HA |
| `DB_URL` | `postgres.apps.svc.cluster.local:5432` | `host:port` Postgresa na Wyse (Service z homelab-infra) |
| `DB_NAME` | `home_assistant` | baza recordera HA |
| `DB_USER` | `mon_home_ass` | rola tylko do odczytu (niżej) |
| `NATS_URL` | `nats://nats.apps.svc.cluster.local:4222` | broker JetStream; brak zmiennej = publisher wyłączony, API działa dalej |
| `NATS_STREAM` / `NATS_SUBJECT` | `HOME_ASSISTANT` / `home-assistant.statistics` | stream tworzony przez aplikację, jeśli nie istnieje (max age 30 dni) |
| `NATS_INTERVAL_SECONDS` | `60` | co ile sprawdzać nowe wiersze `statistics_short_term` |

Redis nie jest wymagany: bez niego cache działa w pamięci poda (`REDIS_HOST`/`REDIS_PORT`, gdy kiedyś będzie).

## secrets.sops.env

```bash
sops .devops/k8s/secrets.sops.env   # edytor; zapis = szyfrowanie
```

```bash
DB_PASSWORD=<openssl rand -hex 24>
```

Broker NATS z uwierzytelnianiem: przenieś `NATS_URL` z `config.env` tutaj jako
`nats://<user>:<hasło>@nats.apps.svc.cluster.local:4222`.
Wcześniej wpisz swój klucz publiczny age w `.sops.yaml` ([setup.md](../../homelab-infra/docs/setup.md), krok 2).

## Postgres: rola tylko do odczytu

Na Wyse (`ssh wyse sudo -u postgres psql -d home_assistant`):

```sql
CREATE ROLE mon_home_ass LOGIN PASSWORD '<DB_PASSWORD>';
ALTER ROLE mon_home_ass SET default_transaction_read_only = on;
GRANT CONNECT ON DATABASE home_assistant TO mon_home_ass;
GRANT USAGE ON SCHEMA public TO mon_home_ass;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mon_home_ass;
-- tabele, które HA utworzy przy przyszłych migracjach
ALTER DEFAULT PRIVILEGES FOR ROLE home_ass_user IN SCHEMA public GRANT SELECT ON TABLES TO mon_home_ass;
```

`pg_hba.conf` dopuszcza dziś z klastra tylko bazę `statistics`. Dopisz (potem `SELECT pg_reload_conf();`):

```
host  home_assistant  mon_home_ass  10.42.0.0/16    scram-sha-256
host  home_assistant  mon_home_ass  <IP RPi5>/32    scram-sha-256
```

## Rejestracja w homelab-infra

```bash
cd ~/IdeaProjects/homelab-infra/clusters/home/apps
cp _app.yaml.example mon-home-ass.yaml && sed -i 's/APP/mon-home-ass/g' mon-home-ass.yaml
git add mon-home-ass.yaml && git commit -m 'feat(apps): mon-home-ass' && git push
```

Paczka GHCR `mon-home-ass` → Package settings → widoczność **Public** (klaster pobiera obrazy bez logowania).

Broker NATS z AuthorizationPolicy: dopuść principal `cluster.local/ns/apps/sa/mon-home-ass`.
