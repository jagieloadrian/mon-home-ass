#!/usr/bin/env bash
set -euo pipefail

vault_path=secret/apps/mon-home-ass

: "${HA_URL:?set HA_URL, e.g. HA_URL=http://<Wyse IP>:8123}"
: "${VAULT_ADDR:?port-forward Vault and run vault login first (homelab-infra docs/vault.md)}"
command -v vault >/dev/null || { echo "vault CLI not found" >&2; exit 1; }

read -rsp "Home Assistant long-lived access token: " token
echo
[ -n "$token" ] || { echo "empty token" >&2; exit 1; }

printf 'Authorization: Bearer %s\n' "$token" | curl -fsS -o /dev/null -H @- "${HA_URL%/}/api/" ||
  { echo "Home Assistant at $HA_URL rejected the token or is unreachable" >&2; exit 1; }

if vault kv metadata get "$vault_path" >/dev/null 2>&1; then
  printf %s "$token" | vault kv patch "$vault_path" HA_TOKEN=- >/dev/null
else
  printf %s "$token" | vault kv put "$vault_path" HA_TOKEN=- >/dev/null
fi
echo "HA_TOKEN stored in $vault_path. The ExternalSecret syncs within 1h; to apply now:"
echo "  kubectl -n apps annotate externalsecret mon-home-ass-secrets force-sync=\"\$(date +%s)\" --overwrite"
echo "  kubectl -n apps rollout restart deploy/mon-home-ass"
