#!/usr/bin/env bash
# Compare ClientHubAI vs Therapy Flow billing list ORDER for a date range.
# Locked order: billing_date DESC, billing id DESC (matches live ClientHub Sep-06:
# Nisreen → Gerson → Miguel). Therapy Flow uses legacy session_billing source ids.
#
# Usage:
#   ./scripts/compare-clienthub-billing-order.sh
#   START_DATE=2026-09-01 END_DATE=2026-09-30 LIMIT=20 ./scripts/compare-clienthub-billing-order.sh
set -euo pipefail

START_DATE="${START_DATE:-2026-09-01}"
END_DATE="${END_DATE:-2026-09-30}"
LIMIT="${LIMIT:-20}"
ORG_ID="${ORG_ID:-50}"
TENANT_SCHEMA="${TENANT_SCHEMA:-tenant_50}"
TZ_NAME="${PRACTICE_TZ:-America/New_York}"
VAULT="${AZURE_KEY_VAULT:-therapyflowkv809}"
PGHOST="${PGHOST:-client-hub.postgres.database.azure.com}"

echo "==> Billing order parity: ClientHubAI vs Therapy Flow"
echo "    range=${START_DATE}..${END_DATE} limit=${LIMIT} org=${ORG_ID} schema=${TENANT_SCHEMA} tz=${TZ_NAME}"

DB_USER="$(az keyvault secret show --vault-name "$VAULT" --name db-username --query value -o tsv)"
DB_PASS="$(az keyvault secret show --vault-name "$VAULT" --name db-password --query value -o tsv)"
export PGPASSWORD="$DB_PASS"
CONN_BASE="host=${PGHOST} port=5432 user=${DB_USER} sslmode=require"

CH_IDS=$(psql "${CONN_BASE} dbname=clienthubai" -Atc "
SELECT sb.id::text
FROM session_billing sb
JOIN sessions s ON s.id = sb.session_id
WHERE DATE(s.session_date) >= DATE '${START_DATE}'
  AND DATE(s.session_date) <= DATE '${END_DATE}'
ORDER BY sb.billing_date DESC, sb.id DESC
LIMIT ${LIMIT};
")

TF_IDS=$(psql "${CONN_BASE} dbname=therapyflow" -Atc "
SET search_path TO ${TENANT_SCHEMA};
SELECT COALESCE(m_b.source_id, 'UNMAPPED-' || sb.id::text)
FROM session_billing sb
JOIN sessions s ON s.id = sb.session_id
LEFT JOIN public.clienthub_legacy_id_mappings m_b
  ON m_b.organisation_id = ${ORG_ID}
 AND m_b.entity_name = 'session_billing'
 AND m_b.target_id = sb.id
WHERE (s.session_date AT TIME ZONE 'UTC' AT TIME ZONE '${TZ_NAME}')::date >= DATE '${START_DATE}'
  AND (s.session_date AT TIME ZONE 'UTC' AT TIME ZONE '${TZ_NAME}')::date <= DATE '${END_DATE}'
ORDER BY sb.billing_date DESC,
         NULLIF(m_b.source_id, '')::bigint DESC NULLS LAST,
         sb.id DESC
LIMIT ${LIMIT};
")

echo
echo "==> ClientHubAI top ${LIMIT} billing ids (billing_date DESC, id DESC)"
echo "$CH_IDS"
echo
echo "==> Therapy Flow top ${LIMIT} (mapped ClientHub billing ids; legacy billing id DESC)"
echo "$TF_IDS"
echo

CH_FILE=$(mktemp)
TF_FILE=$(mktemp)
trap 'rm -f "$CH_FILE" "$TF_FILE"' EXIT
printf '%s\n' "$CH_IDS" >"$CH_FILE"
printf '%s\n' "$TF_IDS" >"$TF_FILE"

TF_SET_FILE=$(mktemp)
trap 'rm -f "$CH_FILE" "$TF_FILE" "$TF_SET_FILE"' EXIT
printf '%s\n' "$TF_IDS" | sed '/^$/d' | sort -u >"$TF_SET_FILE"

MISSING=0
while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  if ! grep -qxF "$id" "$TF_SET_FILE"; then
    echo "MISSING in Therapy Flow (unmapped or not migrated): $id"
    MISSING=$((MISSING + 1))
  fi
done <"$CH_FILE"

CH_SHARED=$(mktemp)
TF_SHARED=$(mktemp)
trap 'rm -f "$CH_FILE" "$TF_FILE" "$TF_SET_FILE" "$CH_SHARED" "$TF_SHARED"' EXIT

while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  if grep -qxF "$id" "$TF_SET_FILE"; then
    echo "$id" >>"$CH_SHARED"
  fi
done <"$CH_FILE"

CH_SET_FILE=$(mktemp)
trap 'rm -f "$CH_FILE" "$TF_FILE" "$TF_SET_FILE" "$CH_SHARED" "$TF_SHARED" "$CH_SET_FILE"' EXIT
printf '%s\n' "$CH_IDS" | sed '/^$/d' | sort -u >"$CH_SET_FILE"

while IFS= read -r id; do
  [[ -z "$id" ]] && continue
  case "$id" in
    UNMAPPED-*) continue ;;
  esac
  if grep -qxF "$id" "$CH_SET_FILE"; then
    echo "$id" >>"$TF_SHARED"
  fi
done <"$TF_FILE"

echo "==> Shared-id order (ClientHub):"
cat "$CH_SHARED"
echo "==> Shared-id order (TherapyFlow):"
cat "$TF_SHARED"

if ! cmp -s "$CH_SHARED" "$TF_SHARED"; then
  echo "FAIL: order mismatch for shared billing ids"
  echo "diff:"
  diff -u "$CH_SHARED" "$TF_SHARED" || true
  exit 1
fi

echo "OK: shared billing id order matches (billing_date DESC, id DESC)"
echo "==> Expected Sep-06 group when present: Nisreen(5094), Gerson(5093), Miguel(5092)"

if [[ "$MISSING" -gt 0 ]]; then
  echo "WARN: $MISSING ClientHub rows missing in Therapy Flow top-${LIMIT} (data gap)"
fi

exit 0
