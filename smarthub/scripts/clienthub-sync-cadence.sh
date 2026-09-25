#!/usr/bin/env bash
# Ongoing ClientHubAI -> TherapyFlow sync cadence for one org.
#
# Notification discipline: in-app notification sync apply only loads rows inside
# CLIENTHUB_MIGRATION_NOTIFICATION_LOOKBACK_MONTHS. If checkpoints for
# notifications / scheduled_notifications (and client_portal) sit older than that
# floor, enqueue floods the queue with rows that become "Source row not found"
# dead letters. Always advance those cursors to the lookback floor before enqueue.
#
# Usage:
#   ./scripts/clienthub-sync-cadence.sh status
#   ./scripts/clienthub-sync-cadence.sh advance-cursors
#   ./scripts/clienthub-sync-cadence.sh run          # enqueue + one apply pass
#   ./scripts/clienthub-sync-cadence.sh drain        # enqueue once, apply until PENDING=0
#
# Required for status/advance-cursors: az CLI logged in (Key Vault DB secrets), or
# PGHOST/PGUSER/PGPASSWORD/DB_* already set.
# Required for run/drain: full ClientHub migration env (see docs), including
# encryption settings used by execute services.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG_ID="${ORG_ID:-50}"
ORG_SLUG="${CLIENTHUB_TARGET_ORG_SLUG:-resiliencecounseling}"
LOOKBACK_MONTHS="${CLIENTHUB_MIGRATION_NOTIFICATION_LOOKBACK_MONTHS:-1}"
SYNC_BATCH_SIZE="${CLIENTHUB_SYNC_BATCH_SIZE:-500}"
MAX_APPLY_PASSES="${MAX_APPLY_PASSES:-40}"
SERVER_PORT="${SERVER_PORT:-18190}"
VAULT="${AZURE_KEY_VAULT:-therapyflowkv809}"
PGHOST="${PGHOST:-client-hub.postgres.database.azure.com}"
SOURCE_DB="${CLIENTHUB_SOURCE_DB_NAME:-clienthubai}"
TARGET_DB="${THERAPYFLOW_DB_NAME:-therapyflow}"
LOG_DIR="${CLIENTHUB_SYNC_LOG_DIR:-/tmp}"

MODE="${1:-}"
if [[ -z "$MODE" || "$MODE" == "-h" || "$MODE" == "--help" ]]; then
  sed -n '2,20p' "$0"
  exit 0
fi

ensure_db_env() {
  if [[ -z "${PGPASSWORD:-}" || -z "${PGUSER:-${DB_USERNAME:-}}" ]]; then
    DB_USER="$(az keyvault secret show --vault-name "$VAULT" --name db-username --query value -o tsv)"
    DB_PASS="$(az keyvault secret show --vault-name "$VAULT" --name db-password --query value -o tsv)"
    export PGUSER="$DB_USER"
    export PGPASSWORD="$DB_PASS"
    export DB_USERNAME="${DB_USERNAME:-$DB_USER}"
    export DB_PASSWORD="${DB_PASSWORD:-$DB_PASS}"
    export CLIENTHUB_SOURCE_DB_USERNAME="${CLIENTHUB_SOURCE_DB_USERNAME:-$DB_USER}"
    export CLIENTHUB_SOURCE_DB_PASSWORD="${CLIENTHUB_SOURCE_DB_PASSWORD:-$DB_PASS}"
  fi
  export PGUSER="${PGUSER:-${DB_USERNAME}}"
  TARGET_CONN="host=${PGHOST} port=5432 dbname=${TARGET_DB} user=${PGUSER} sslmode=require"
}

ensure_encryption_env() {
  if [[ "${APP_ENCRYPTION_PROVIDER:-}" == "keyvault" ]]; then
    export AZURE_KEYVAULT_URI="${AZURE_KEYVAULT_URI:-https://${VAULT}.vault.azure.net/}"
    export APP_ENCRYPTION_KEK_NAME="${APP_ENCRYPTION_KEK_NAME:-therapyflow-phi-dek}"
    export APP_SEARCH_HMAC_KEY_NAME="${APP_SEARCH_HMAC_KEY_NAME:-tfenc-search-hmac-key}"
    export APP_ENCRYPTION_KEY_ID="${APP_ENCRYPTION_KEY_ID:-primary}"
    export APP_ENCRYPTION_PREVIOUS_KEYS="${APP_ENCRYPTION_PREVIOUS_KEYS:-openssl=therapyflow-phi-dek-openssl,clienthubmig=therapyflow-phi-dek-clienthub-mig}"
    return 0
  fi
  if [[ -n "${APP_ENCRYPTION_MASTER_KEY:-}" || -n "${ENCRYPTION_PASSWORD:-}" ]]; then
    export APP_ENCRYPTION_PROVIDER="${APP_ENCRYPTION_PROVIDER:-config}"
    return 0
  fi
  # Local/cron default: pull PHI DEK + encryptor password from Key Vault (az login required).
  local master enc
  master="$(az keyvault secret show --vault-name "$VAULT" --name therapyflow-phi-dek --query value -o tsv)"
  enc="$(az keyvault secret show --vault-name "$VAULT" --name encryption-password --query value -o tsv)"
  export APP_ENCRYPTION_PROVIDER=config
  export APP_ENCRYPTION_MASTER_KEY="$master"
  export ENCRYPTION_PASSWORD="$enc"
  export JASYPT_ENCRYPTOR_PASSWORD="${JASYPT_ENCRYPTOR_PASSWORD:-$enc}"
  export APP_PHI_ENCRYPTION_BACKFILL_ENABLED="${APP_PHI_ENCRYPTION_BACKFILL_ENABLED:-false}"
  export JWT_SECRET="${JWT_SECRET:-local-dev-jwt-secret-change-me}"
}

psql_target() {
  psql "$TARGET_CONN" "$@"
}

show_status() {
  ensure_db_env
  echo "==> Sync status org=${ORG_ID} lookback_months=${LOOKBACK_MONTHS}"
  psql_target -c "
SELECT entity_name, cursor_value, updated_at
FROM public.clienthub_sync_checkpoints
WHERE organisation_id = ${ORG_ID} AND source_system = 'ClientHubAI'
  AND entity_name IN (
    'notifications', 'scheduled_notifications', 'client_portal',
    'sessions', 'session_notes', 'documents', 'clients',
    'assessment_responses', 'tasks', 'session_billing'
  )
ORDER BY entity_name;

SELECT status, count(*)
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID}
GROUP BY 1
ORDER BY 1;

SELECT count(*) AS pending_due
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID}
  AND status = 'PENDING'
  AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP);
"
}

advance_cursors() {
  ensure_db_env
  echo "==> Advancing lookback-sensitive cursors to floor (now - ${LOOKBACK_MONTHS} months)"
  # Also nudge assessment_responses if it is more than 14 days behind so cadence
  # does not replay a huge already-mapped historical UPDATE backlog.
  psql_target -v ON_ERROR_STOP=1 <<SQL
WITH floor AS (
  SELECT (CURRENT_TIMESTAMP - make_interval(months => ${LOOKBACK_MONTHS})) AS ts
),
recent AS (
  SELECT (CURRENT_TIMESTAMP - interval '14 days') AS ts
),
targets(entity_name, floor_kind) AS (
  VALUES
    ('notifications', 'lookback'),
    ('scheduled_notifications', 'lookback'),
    ('client_portal', 'lookback'),
    ('assessment_responses', 'recent')
)
UPDATE public.clienthub_sync_checkpoints c
SET cursor_value = to_char(
      CASE t.floor_kind
        WHEN 'lookback' THEN (SELECT ts FROM floor)
        ELSE (SELECT ts FROM recent)
      END AT TIME ZONE 'UTC',
      'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'
    ),
    updated_at = CURRENT_TIMESTAMP
FROM targets t
WHERE c.organisation_id = ${ORG_ID}
  AND c.source_system = 'ClientHubAI'
  AND c.entity_name = t.entity_name
  AND (
    c.cursor_value IS NULL
    OR c.cursor_value::timestamptz < CASE t.floor_kind
      WHEN 'lookback' THEN (SELECT ts FROM floor)
      ELSE (SELECT ts FROM recent)
    END
  );

SELECT entity_name, cursor_value, updated_at
FROM public.clienthub_sync_checkpoints
WHERE organisation_id = ${ORG_ID} AND source_system = 'ClientHubAI'
  AND entity_name IN ('notifications', 'scheduled_notifications', 'client_portal', 'assessment_responses')
ORDER BY entity_name;
SQL
}

run_spring_sync() {
  local enqueue="$1"
  local apply="$2"
  local log_file="$3"

  export CLIENTHUB_MIGRATION_ENABLED=true
  export CLIENTHUB_MIGRATION_DRY_RUN=false
  export CLIENTHUB_SYNC_ENABLED=true
  export CLIENTHUB_SYNC_ENQUEUE_EVENTS="$enqueue"
  export CLIENTHUB_SYNC_APPLY_EVENTS="$apply"
  export CLIENTHUB_SYNC_BATCH_SIZE="$SYNC_BATCH_SIZE"
  export CLIENTHUB_TARGET_ORG_SLUG="$ORG_SLUG"
  export CLIENTHUB_MIGRATION_NOTIFICATION_LOOKBACK_MONTHS="$LOOKBACK_MONTHS"
  export CLIENTHUB_SOURCE_DB_URL="${CLIENTHUB_SOURCE_DB_URL:-jdbc:postgresql://${PGHOST}:5432/${SOURCE_DB}?sslmode=require}"
  export DB_URL="${DB_URL:-jdbc:postgresql://${PGHOST}:5432/${TARGET_DB}?sslmode=require}"
  export APP_REDIS_ENABLED="${APP_REDIS_ENABLED:-false}"
  export APP_REDIS_FAIL_OPEN="${APP_REDIS_FAIL_OPEN:-true}"
  export SPRING_FLYWAY_ENABLED="${SPRING_FLYWAY_ENABLED:-false}"
  export SPRING_TASK_SCHEDULING_ENABLED="${SPRING_TASK_SCHEDULING_ENABLED:-false}"
  export SPRING_QUARTZ_AUTO_STARTUP="${SPRING_QUARTZ_AUTO_STARTUP:-false}"
  export TENANT_JOBS_MIGRATION_ENABLED="${TENANT_JOBS_MIGRATION_ENABLED:-false}"
  export NOTIFICATION_EMAIL_ENABLED="${NOTIFICATION_EMAIL_ENABLED:-false}"
  export SERVER_PORT

  ensure_encryption_env

  echo "==> spring-boot sync enqueue=${enqueue} apply=${apply} port=${SERVER_PORT} log=${log_file}"
  (
    cd "$PROJECT_ROOT"
    # Avoid NoClassDefFoundError across rapid restarts after concurrent compiles/deploys.
    ./mvnw -q -DskipTests compile
    ./mvnw -DskipTests spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Dlogging.level.com.smart.therapy.flow.common.logging.MethodLoggingAspect=WARN -Dlogging.level.org.hibernate.SQL=WARN -Dlogging.level.com.smart.therapy.flow.common.service.EncryptionService=ERROR"
  ) >"$log_file" 2>&1
}

pending_due_count() {
  ensure_db_env
  psql_target -Atc "
SELECT count(*)
FROM public.clienthub_sync_events
WHERE organisation_id = ${ORG_ID}
  AND status = 'PENDING'
  AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP);
"
}

case "$MODE" in
  status)
    show_status
    ;;
  advance-cursors)
    advance_cursors
    ;;
  run)
    ensure_db_env
    advance_cursors
    run_spring_sync true true "${LOG_DIR}/clienthub-sync-cadence-run.log"
    show_status
    ;;
  drain)
    ensure_db_env
    advance_cursors
    run_spring_sync true true "${LOG_DIR}/clienthub-sync-cadence-pass1.log"
    pass=1
    while true; do
      due="$(pending_due_count)"
      echo "==> pass=${pass} pending_due=${due}"
      if [[ "$due" == "0" ]]; then
        echo "QUEUE_CLEAR"
        break
      fi
      if (( pass >= MAX_APPLY_PASSES )); then
        echo "ERROR: hit MAX_APPLY_PASSES=${MAX_APPLY_PASSES} with pending_due=${due}" >&2
        exit 1
      fi
      pass=$((pass + 1))
      SERVER_PORT=$((SERVER_PORT + 1))
      run_spring_sync false true "${LOG_DIR}/clienthub-sync-cadence-pass${pass}.log"
    done
    show_status
    ;;
  *)
    echo "Unknown mode: $MODE (expected status|advance-cursors|run|drain)" >&2
    exit 1
    ;;
esac
