#!/usr/bin/env bash
# Azure Container Apps deployment for TherapyFlow.
# Usage:
#   ./scripts/deploy-azure.sh              # build + deploy
#   ./scripts/deploy-azure.sh --build-only # ACR build only (CI build stage)
#   ./scripts/deploy-azure.sh --deploy-only # deploy existing image (CI deploy stage)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=scripts/notify-sparkpost.sh
source "$SCRIPT_DIR/notify-sparkpost.sh"

# Azure resources (override via env / GitLab CI variables)
RG="${AZURE_RESOURCE_GROUP:-Resilience}"
ACR="${AZURE_ACR:-therapyflowacr809}"
REPO="${AZURE_IMAGE_REPO:-therapy-flow}"
APP="${AZURE_CONTAINER_APP:-therapy-flow-api}"
SUBSCRIPTION="${AZURE_SUBSCRIPTION:-80920a1f-e61e-4c65-9ecf-bb5bc6115046}"
DEPLOY_ENV="${DEPLOY_ENV:-prod}"
IMAGE_TAG="${IMAGE_TAG:-manual-$(date +%Y%m%d%H%M%S)}"

# Keep APP_PAGINATION_MAX_PAGE_SIZE >= 500 on the Container App so month
# scheduling (view=calendar) can load a full busy month in one request.
# Clamping below 500 truncates ASC calendar pages and blanks later dates.

BUILD_ONLY=false
DEPLOY_ONLY=false
DEPLOY_SUCCEEDED=false
FAILURE_NOTIFIED=false

usage() {
  echo "Usage: $0 [--build-only | --deploy-only]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-only) BUILD_ONLY=true; shift ;;
    --deploy-only) DEPLOY_ONLY=true; shift ;;
    -h|--help) usage ;;
    *) echo "Unknown option: $1"; usage ;;
  esac
done

if $BUILD_ONLY && $DEPLOY_ONLY; then
  echo "ERROR: --build-only and --deploy-only are mutually exclusive"
  exit 1
fi

notify_failure() {
  if $FAILURE_NOTIFIED; then
    return
  fi
  FAILURE_NOTIFIED=true
  local reason="${1:-Deployment failed}"
  local details="${2:-}"
  send_sparkpost_notification "failure" "$DEPLOY_ENV" "$reason" "$details" || true
}

on_error() {
  local exit_code=$?
  local line="${1:-unknown}"
  notify_failure "Deployment failed at line ${line} (exit ${exit_code})" "$(tail -30 /tmp/deploy-azure.log 2>/dev/null || echo '')"
  exit "$exit_code"
}

trap 'on_error $LINENO' ERR

exec > >(tee /tmp/deploy-azure.log) 2>&1

azure_login() {
  echo "==> Authenticating with Azure"
  if [[ -n "${AZURE_CLIENT_ID:-}" && -n "${AZURE_CLIENT_SECRET:-}" && -n "${AZURE_TENANT_ID:-}" ]]; then
    az login --service-principal \
      -u "$AZURE_CLIENT_ID" \
      -p "$AZURE_CLIENT_SECRET" \
      --tenant "$AZURE_TENANT_ID" \
      --output none
  elif az account show &>/dev/null; then
    echo "Using existing Azure CLI session"
  else
    echo "ERROR: Azure credentials not configured. Set AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID"
    exit 1
  fi
  az account set --subscription "$SUBSCRIPTION"
}

build_image() {
  echo "==> Building image ${REPO}:${IMAGE_TAG} in ACR (${ACR})"
  az acr build \
    --registry "$ACR" \
    --resource-group "$RG" \
    --image "${REPO}:${IMAGE_TAG}" \
    --file Dockerfile \
    "$PROJECT_ROOT"
  echo "==> Image built: ${REPO}:${IMAGE_TAG}"
}

get_image_uri() {
  local login_server
  login_server="$(az acr show -n "$ACR" -g "$RG" --query loginServer -o tsv)"
  echo "${login_server}/${REPO}:${IMAGE_TAG}"
}

wait_for_revision() {
  local revision="$1"
  local max_attempts="${DEPLOY_WAIT_ATTEMPTS:-60}"
  local attempt=0

  echo "==> Waiting for revision ${revision} to become healthy"
  while [[ $attempt -lt $max_attempts ]]; do
    attempt=$((attempt + 1))

    local provisioning_state running_status health_state active_replicas
    provisioning_state="$(az containerapp revision show \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "properties.provisioningState" -o tsv 2>/dev/null || echo "Unknown")"
    health_state="$(az containerapp revision show \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "properties.healthState" -o tsv 2>/dev/null || echo "Unknown")"
    running_status="$(az containerapp show -n "$APP" -g "$RG" \
      --query "properties.runningStatus" -o tsv 2>/dev/null || echo "Unknown")"
    active_replicas="$(az containerapp replica list \
      -n "$APP" -g "$RG" --revision "$revision" \
      --query "length([?properties.runningState=='Running'])" -o tsv 2>/dev/null || echo "0")"

    echo "[${attempt}/${max_attempts}] provisioning=${provisioning_state} health=${health_state} running=${running_status} active_replicas=${active_replicas}"

    if [[ "$provisioning_state" == "Provisioned" && "$running_status" == "Running" && "${active_replicas:-0}" -gt 0 ]]; then
      if [[ "$health_state" == "Healthy" || "$health_state" == "None" || "$health_state" == "Unknown" ]]; then
        echo "==> Revision is running"
        return 0
      fi
    fi

    if [[ "$health_state" == "Unhealthy" ]]; then
      echo "ERROR: Revision reported unhealthy"
      return 1
    fi

    sleep 10
  done

  echo "ERROR: Revision did not become healthy within timeout"
  return 1
}

check_log_streams() {
  local revision="$1"
  echo "==> Verifying deployment via log streams (revision: ${revision})"

  echo "--- Console logs (last 50 lines) ---"
  local console_logs=""
  console_logs="$(az containerapp logs show \
    -n "$APP" -g "$RG" \
    --revision "$revision" \
    --type console \
    --tail 50 \
    --follow false 2>/dev/null || echo "")"

  if [[ -z "$console_logs" ]]; then
    echo "WARN: Could not fetch console logs — trying without revision filter"
    console_logs="$(az containerapp logs show \
      -n "$APP" -g "$RG" \
      --type console \
      --tail 50 \
      --follow false 2>/dev/null || echo "")"
  fi

  echo "$console_logs"

  echo "--- System logs (last 20 lines) ---"
  az containerapp logs show \
    -n "$APP" -g "$RG" \
    --type system \
    --tail 20 \
    --follow false 2>/dev/null || echo "WARN: Could not fetch system logs"

  if echo "$console_logs" | grep -qiE "Started .* in |Tomcat started on port|Netty started on port"; then
    echo "==> Application startup confirmed in log streams"
    return 0
  fi

  if echo "$console_logs" | grep -qiE "APPLICATION FAILED TO START|Error creating bean|Exception in thread"; then
    echo "ERROR: Application startup failure detected in logs"
    return 1
  fi

  echo "WARN: Could not confirm application startup in logs — relying on health endpoint"
  return 0
}

health_check() {
  local fqdn="$1"
  local max_attempts="${HEALTH_CHECK_ATTEMPTS:-12}"
  local attempt=0
  local url="https://${fqdn}/actuator/health"

  echo "==> Health check: ${url}"
  while [[ $attempt -lt $max_attempts ]]; do
    attempt=$((attempt + 1))
    local status
    status="$(curl -s -o /tmp/health-response.json -w "%{http_code}" --max-time 15 "$url" 2>/dev/null || echo "000")"
    echo "[${attempt}/${max_attempts}] HTTP ${status}"

    if [[ "$status" == "200" ]]; then
      echo "==> Health check passed"
      cat /tmp/health-response.json 2>/dev/null || true
      return 0
    fi

    sleep 10
  done

  echo "ERROR: Health check failed after ${max_attempts} attempts"
  return 1
}

deploy_image() {
  local image="$1"

  echo "==> Updating container app ${APP} with ${image}"
  az containerapp update -n "$APP" -g "$RG" --image "$image" -o table

  local latest_revision
  latest_revision="$(az containerapp show -n "$APP" -g "$RG" --query properties.latestRevisionName -o tsv)"
  echo "==> Restarting revision ${latest_revision}"
  az containerapp revision restart -n "$APP" -g "$RG" --revision "$latest_revision"

  echo "==> Deployment status"
  az containerapp revision show -n "$APP" -g "$RG" --revision "$latest_revision" -o table

  local fqdn
  fqdn="$(az containerapp show -n "$APP" -g "$RG" \
    --query "properties.configuration.ingress.fqdn" -o tsv)"

  az containerapp show -n "$APP" -g "$RG" \
    --query "{fqdn:properties.configuration.ingress.fqdn,latestRevision:properties.latestRevisionName,image:properties.template.containers[0].image,runningStatus:properties.runningStatus}" \
    -o json

  wait_for_revision "$latest_revision"
  check_log_streams "$latest_revision"
  health_check "$fqdn"
}

main() {
  echo "==> TherapyFlow Azure deployment"
  echo "    Environment: ${DEPLOY_ENV}"
  echo "    Resource group: ${RG}"
  echo "    Container app: ${APP}"
  echo "    Image tag: ${IMAGE_TAG}"

  azure_login

  if ! $DEPLOY_ONLY; then
    build_image
  fi

  if ! $BUILD_ONLY; then
    local image
    image="$(get_image_uri)"
    deploy_image "$image"
    DEPLOY_SUCCEEDED=true
    trap - ERR
    send_sparkpost_notification "success" "$DEPLOY_ENV" \
      "Deployment completed and verified (health check + log streams)" \
      "Image: ${image}\nFQDN: $(az containerapp show -n "$APP" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)"
    echo "==> Deployment completed successfully"
  else
    trap - ERR
    echo "==> Build completed successfully"
  fi
}

main "$@"
