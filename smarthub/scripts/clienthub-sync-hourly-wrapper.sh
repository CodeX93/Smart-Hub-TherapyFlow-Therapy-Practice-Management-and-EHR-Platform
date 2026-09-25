#!/usr/bin/env bash
# Hourly ClientHubAI -> TherapyFlow sync wrapper for ops servers.
# Loads a secured env file, runs drain, emails a stats report.
#
# Required env file (chmod 600), default:
#   $HOME/.config/clienthub-sync/env
#
# Optional overrides:
#   CLIENTHUB_SYNC_ENV_FILE
#   CLIENTHUB_SYNC_EMAIL_TO   (default aqeel@mi6.global)
#   CLIENTHUB_SYNC_EMAIL_FROM (default noreply@mail.resiliencecrm.com)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="${CLIENTHUB_SYNC_ENV_FILE:-$HOME/.config/clienthub-sync/env}"
LOG_DIR="${CLIENTHUB_SYNC_LOG_DIR:-$HOME/logs/clienthub-sync}"
LOCK_FILE="${CLIENTHUB_SYNC_LOCK_FILE:-$HOME/.cache/clienthub-sync.lock}"
EMAIL_TO="${CLIENTHUB_SYNC_EMAIL_TO:-aqeel@mi6.global}"
EMAIL_FROM="${CLIENTHUB_SYNC_EMAIL_FROM:-noreply@mail.resiliencecrm.com}"
EMAIL_FROM_NAME="${CLIENTHUB_SYNC_EMAIL_FROM_NAME:-SmartHub Sync}"
ORG_ID="${ORG_ID:-50}"
ORG_SLUG="${CLIENTHUB_TARGET_ORG_SLUG:-resiliencecounseling}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
RUN_LOG="${LOG_DIR}/hourly-${STAMP}.log"
STATS_FILE="${LOG_DIR}/hourly-${STAMP}.stats.txt"
mkdir -p "$LOG_DIR" "$(dirname "$LOCK_FILE")"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: missing env file: $ENV_FILE" >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

export ORG_ID ORG_SLUG
export CLIENTHUB_TARGET_ORG_SLUG="${CLIENTHUB_TARGET_ORG_SLUG:-$ORG_SLUG}"
export CLIENTHUB_SYNC_LOG_DIR="$LOG_DIR"
export MAX_APPLY_PASSES="${MAX_APPLY_PASSES:-40}"
export SERVER_PORT="${SERVER_PORT:-18192}"
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH:/usr/local/bin:/usr/bin:/bin"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "Another clienthub sync is already running; exiting." | tee -a "$RUN_LOG"
  exit 0
fi

START_EPOCH="$(date +%s)"
STATUS="SUCCEEDED"
EXIT_CODE=0

collect_stats() {
  local conn="host=${PGHOST:-client-hub.postgres.database.azure.com} port=5432 dbname=${THERAPYFLOW_DB_NAME:-therapyflow} user=${PGUSER:-${DB_USERNAME}} sslmode=require"
  {
    echo "ClientHub sync hourly report"
    echo "============================"
    echo "host: $(hostname)"
    echo "org_id: ${ORG_ID}"
    echo "org_slug: ${CLIENTHUB_TARGET_ORG_SLUG}"
    echo "finished_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "duration_sec: $(( $(date +%s) - START_EPOCH ))"
    echo "status: ${STATUS}"
    echo "exit_code: ${EXIT_CODE}"
    echo "run_log: ${RUN_LOG}"
    echo
    echo "Sync events by status"
    echo "--------------------"
    PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}" psql "$conn" -c "
SELECT status, count(*) AS n, max(updated_at) AS last_updated
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID}
GROUP BY status
ORDER BY n DESC;"
    echo
    echo "Pending due events"
    echo "------------------"
    PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}" psql "$conn" -c "
SELECT count(*) AS pending_due
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID}
  AND status = 'PENDING'
  AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP);"
    echo
    echo "Key checkpoints"
    echo "---------------"
    PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}" psql "$conn" -c "
SELECT entity_name, status, cursor_value, last_successful_sync_at,
       round(extract(epoch from (now() - last_successful_sync_at))/3600.0, 1) AS hours_since_success
FROM public.clienthub_sync_checkpoints
WHERE organisation_id = ${ORG_ID}
  AND entity_name IN (
    'sessions','session_notes','session_billing','clients','documents',
    'notifications','scheduled_notifications','assessment_responses','tasks'
  )
ORDER BY entity_name;"
    echo
    echo "Mapped row totals (top entities)"
    echo "--------------------------------"
    PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}" psql "$conn" -c "
SELECT entity_name, count(*) AS mapped, max(updated_at) AS last_updated
FROM public.clienthub_legacy_id_mappings
WHERE organisation_id = ${ORG_ID}
GROUP BY entity_name
ORDER BY mapped DESC
LIMIT 15;"
    echo
    echo "Recent dead letters (sample)"
    echo "----------------------------"
    PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-}}" psql "$conn" -c "
SELECT entity_name, event_type, source_id,
       left(coalesce(error_message,''), 120) AS err,
       updated_at
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID} AND status = 'DEAD_LETTER'
ORDER BY updated_at DESC
LIMIT 10;"
    if [[ "$STATUS" != "SUCCEEDED" ]]; then
      echo
      echo "Tail of run log"
      echo "---------------"
      tail -n 80 "$RUN_LOG" || true
    fi
  } >"$STATS_FILE" 2>&1 || {
    echo "Failed to collect DB stats" >"$STATS_FILE"
    tail -n 40 "$RUN_LOG" >>"$STATS_FILE" || true
  }
}

send_email() {
  local subject="$1"
  local body_file="$2"
  if [[ -z "${SPARKPOST_API_KEY:-}" ]]; then
    echo "WARN: SPARKPOST_API_KEY unset; skipping email" | tee -a "$RUN_LOG"
    return 0
  fi
  python3 - "$subject" "$body_file" <<'PY' || echo "WARN: email send failed" | tee -a "$RUN_LOG"
import json, os, sys, urllib.request
subject, body_file = sys.argv[1], sys.argv[2]
with open(body_file, encoding="utf-8", errors="replace") as f:
    text = f.read()
payload = {
    "options": {"click_tracking": False, "open_tracking": False},
    "content": {
        "from": {
            "name": os.environ.get("EMAIL_FROM_NAME", "SmartHub Sync"),
            "email": os.environ.get("CLIENTHUB_SYNC_EMAIL_FROM", os.environ.get("EMAIL_FROM", "noreply@mail.resiliencecrm.com")),
        },
        "subject": subject,
        "text": text,
    },
    "recipients": [{"address": {"email": os.environ.get("CLIENTHUB_SYNC_EMAIL_TO", "aqeel@mi6.global")}}],
}
req = urllib.request.Request(
    os.environ.get("SPARKPOST_BASE_URL", "https://api.sparkpost.com/api/v1") + "/transmissions",
    data=json.dumps(payload).encode("utf-8"),
    headers={
        "Authorization": os.environ["SPARKPOST_API_KEY"],
        "Content-Type": "application/json",
    },
    method="POST",
)
with urllib.request.urlopen(req, timeout=60) as resp:
    print(resp.read().decode("utf-8", errors="replace"))
PY
}

{
  echo "==> $(date -u +%Y-%m-%dT%H:%M:%SZ) starting clienthub hourly drain"
  echo "project=$PROJECT_ROOT env=$ENV_FILE"
} | tee -a "$RUN_LOG"

set +e
(
  cd "$PROJECT_ROOT"
  ./scripts/clienthub-sync-cadence.sh drain
) >>"$RUN_LOG" 2>&1
EXIT_CODE=$?
set -e

if [[ "$EXIT_CODE" -ne 0 ]]; then
  STATUS="FAILED"
fi

collect_stats
SUBJECT="[ClientHub Sync] ${STATUS} org=${ORG_ID} ${ORG_SLUG} ${STAMP}"
export CLIENTHUB_SYNC_EMAIL_TO="$EMAIL_TO"
export CLIENTHUB_SYNC_EMAIL_FROM="$EMAIL_FROM"
export EMAIL_FROM_NAME
send_email "$SUBJECT" "$STATS_FILE" | tee -a "$RUN_LOG"

echo "==> $(date -u +%Y-%m-%dT%H:%M:%SZ) finished status=${STATUS} exit=${EXIT_CODE}" | tee -a "$RUN_LOG"
exit "$EXIT_CODE"
